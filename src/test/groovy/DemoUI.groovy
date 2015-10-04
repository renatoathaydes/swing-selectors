import com.athaydes.automaton.ReflectionHelper
import com.athaydes.automaton.swing.selectors.SwingNavigator
import com.athaydes.automaton.swing.selectors.SwingSelector
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JTable
import javax.swing.JTextArea
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionListener

import static java.awt.GridBagConstraints.HORIZONTAL
import static java.awt.GridBagConstraints.NONE

/**
 *
 */
class DemoUI {

    final builder = new SwingBuilder()
    final client = new WeatherApiClient()
    final forecastData = [ ]

    static queryForLocation( String location ) {
        """|select item
           |from weather.forecast
           |where woeid in
           |  (select woeid from geo.places(1)
           |   where text='$location')
           |and u='c'""".stripMargin()
    }

    void show() {
        // async method chaining
        createUI this.&setupUILogic, this.&styleUI
    }

    private static void consumeNextAction( arg, Closure... nextActions ) {
        if ( nextActions ) {
            nextActions.first().call( arg, nextActions.tail() )
        }
    }

    private void styleUI( JFrame frame, Closure... nextActions ) {
        def mainFont = new Font( 'Helvetica', Font.PLAIN, 14 )
        SwingNavigator.navigateBreadthFirst( frame ) { component ->
            ReflectionHelper.callMethodIfExists( component,
                    'setFont', mainFont )
        }
        final S = new SwingSelector( root: frame )

        def queryField = S.selectWithName( 'query-field' ) as JTextArea
        queryField.font = new Font( 'Courier', Font.PLAIN, 14 )
        consumeNextAction( frame, nextActions )
    }

    void setupUILogic( JFrame frame, Closure... nextActions ) {
        final S = new SwingSelector( root: frame )

        def queryField = S.selectWithName( 'query-field' ) as JTextArea
        queryField.text = queryForLocation( 'Stockholm, Sweden' )

        def table = S.selectWithType( JTable )

        def runButton = S.selectWithName( 'run-button' ) as JButton
        runButton.addActionListener { event ->
            Thread.start {
                final response = client.run( queryField.text )
                builder.edt {
                    forecastData.clear()
                    forecastData.addAll response.forecast
                    table.revalidate()
                    table.repaint()
                }
            }
        } as ActionListener
        consumeNextAction( frame, nextActions )
    }

    void createUI( Closure... nextActions ) {
        def tableColumns = { List<Map> columns ->
            columns.each { col ->
                builder.propertyColumn(
                        header: col.name,
                        propertyName: col.property,
                        editable: false )
            }
        }
        builder.edt {
            consumeNextAction frame( title: 'Yahoo! Weather API Client',
                    size: [ 350, 350 ] as Dimension,
                    defaultCloseOperation: JFrame.EXIT_ON_CLOSE,
                    locationRelativeTo: null,
                    show: true ) {
                scrollPane {
                    panel {
                        gridBagLayout()
                        label( 'Y! query:',
                                constraints: gbc( gridx: 1, gridy: 1, ipady: 4 ) )
                        textArea( name: 'query-field', rows: 5, columns: 30,
                                constraints: gbc( gridx: 1, gridy: 2, ipady: 4, fill: HORIZONTAL ) )
                        button( 'Run', name: 'run-button', preferredSize: [ 100, 25 ] as Dimension,
                                constraints: gbc( gridx: 1, gridy: 3, fill: NONE ) )
                        def t1 = table( name: 'forecast-table',
                                constraints: gbc( gridx: 1, gridy: 5, ipady: 4, fill: HORIZONTAL ) ) {
                            tableModel( list: forecastData ) {
                                tableColumns( [
                                        [ name: 'Date', property: 'date' ],
                                        [ name: 'Low', property: 'low' ],
                                        [ name: 'High', property: 'high' ],
                                        [ name: 'Conditions', property: 'text' ]
                                ] )
                            }
                        }
                        widget( constraints: gbc( gridx: 1, gridy: 4, fill: HORIZONTAL ), t1.tableHeader )
                    }
                }
            }, nextActions
        }
    }

    static main( args ) {
        new DemoUI().show()
    }

}

class WeatherApiClient {

    def run( String query ) {
        def connection = new URL( "https://query.yahooapis.com/v1/public/yql?q=" +
                URLEncoder.encode( query, 'UTF-8' ) )
                .openConnection() as HttpURLConnection
        connection.setRequestProperty( 'Accept', 'application/json' )

        if ( connection.responseCode == 200 ) {
            def json = connection.inputStream.withCloseable { inStream ->
                new JsonSlurper().parse( inStream as InputStream )
            }
            println "Full response: $json"
            return json.query.results.channel.item
        } else {
            throw new RuntimeException( "Failed request: ${connection.responseCode}" +
                    " - ${connection.inputStream.text}" )
        }
    }
}