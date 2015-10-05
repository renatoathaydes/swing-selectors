import com.athaydes.automaton.ReflectionHelper
import com.athaydes.automaton.swing.selectors.SwingNavigator
import com.athaydes.automaton.swing.selectors.SwingSelector
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextArea
import java.awt.Color
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

        def status = S.selectWithName( 'status-label' ) as JLabel
        status.font = new Font( 'Helvetica', Font.ITALIC, 14 )

        consumeNextAction( frame, nextActions )
    }

    void setupUILogic( JFrame frame, Closure... nextActions ) {
        final S = new SwingSelector( root: frame )

        def queryField = S.selectWithName( 'query-field' ) as JTextArea
        queryField.text = queryForLocation( 'Stockholm, Sweden' )

        def runButton = S.selectWithName( 'run-button' ) as JButton
        runButton.addActionListener { event ->
            client.run( queryField.text ) { response ->
                handleApiResponse( response,
                        S.selectWithType( JTable ),
                        S.selectWithName( 'status-label' ) as JLabel,
                        S.selectWithName( 'current-weather' ) as JLabel )
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
                        label( name: 'status-label',
                                constraints: gbc( gridx: 1, gridy: 4, fill: HORIZONTAL ) )
                        label( name: 'current-weather',
                                constraints: gbc( gridx: 1, gridy: 5, fill: HORIZONTAL ) )
                        def t1 = table( name: 'forecast-table',
                                constraints: gbc( gridx: 1, gridy: 7, ipady: 4, fill: HORIZONTAL ) ) {
                            tableModel( list: forecastData ) {
                                tableColumns( [
                                        [ name: 'Date', property: 'date' ],
                                        [ name: 'Low', property: 'low' ],
                                        [ name: 'High', property: 'high' ],
                                        [ name: 'Conditions', property: 'text' ]
                                ] )
                            }
                        }
                        widget( constraints: gbc( gridx: 1, gridy: 6, fill: HORIZONTAL ), t1.tableHeader )
                    }
                }
            }, nextActions
        }
    }

    void handleApiResponse( response, JTable table, JLabel status, JLabel currentWeather ) {
        if ( response instanceof Throwable ) {
            builder.edt {
                status.text = response.message
                status.foreground = Color.RED
            }
        } else {
            builder.edt {
                status.text = response.title
                status.foreground = Color.BLACK
                currentWeather.text = "Currently: ${response.condition.temp}C, ${response.condition.text}"
                forecastData.clear()
                forecastData.addAll response.forecast
                table.revalidate()
                table.repaint()
            }
        }
    }

    static main( args ) {
        new DemoUI().show()
    }

}

class WeatherApiClient {

    def run( String query, Closure callback ) {
        def connection = new URL( "https://query.yahooapis.com/v1/public/yql?q=" +
                URLEncoder.encode( query, 'UTF-8' ) )
                .openConnection() as HttpURLConnection
        connection.setRequestProperty( 'Accept', 'application/json' )

        Thread.start {
            if ( connection.responseCode == 200 ) {
                def json = connection.inputStream.withCloseable { inStream ->
                    new JsonSlurper().parse( inStream as InputStream )
                }
                println "Full response: $json"
                callback json.query.results.channel.item
            } else {
                callback new RuntimeException( "Failed request: ${connection.responseCode}" +
                        " - ${connection.inputStream.text}" )
            }
        }
    }
}