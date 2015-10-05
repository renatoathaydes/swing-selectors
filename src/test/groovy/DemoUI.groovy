import com.athaydes.automaton.ReflectionHelper
import com.athaydes.automaton.swing.selectors.SwingNavigator
import com.athaydes.automaton.swing.selectors.SwingSelector
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextArea
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import java.awt.event.ActionListener

import static java.awt.GridBagConstraints.BOTH
import static java.awt.GridBagConstraints.HORIZONTAL
import static java.awt.GridBagConstraints.NONE
import static java.awt.GridBagConstraints.VERTICAL

/**
 *
 */
class DemoUI {

    static final defaultForeground = Color.WHITE
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

        ( S.selectWithName( 'main-panel' ) as JPanel ).with {
            background = new Color( 138, 43, 226 )
        }

        S.selectAllWithType( JLabel ).each { label ->
            label.foreground = defaultForeground
        }

        ( S.selectWithName( 'query-field' ) as JTextArea ).with {
            font = new Font( 'sans-serif', Font.PLAIN, 14 )
            foreground = new Color( 55, 209, 55 )
        }

        def status = S.selectWithName( 'status-label' ) as JLabel
        status.font = new Font( 'Helvetica', Font.ITALIC, 14 )

        ( S.selectWithName( 'run-button' ) as JButton ).with {
            background = new Color( 0, 191, 255 )
            foreground = Color.WHITE
        }

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
        final defaultInsets = new Insets( 4, 4, 4, 4 )
        final cnst = { Map c ->
            builder.gbc( gridx: c.row, gridy: c.col, insets: defaultInsets,
                    ipadx: c.ipadx ?: 0, ipady: c.ipady ?: 0,
                    fill: c.fill ?: NONE,
                    weightx: c.fill in [ HORIZONTAL, BOTH ] ? 1.0 : 0.0,
                    weighty: c.fill in [ VERTICAL, BOTH ] ? 1.0 : 0.0 )
        }

        builder.edt {
            def jframe = frame( title: 'Yahoo! Weather API Client',
                    size: [ 500, 400 ] as Dimension,
                    defaultCloseOperation: JFrame.EXIT_ON_CLOSE,
                    locationRelativeTo: null,
                    show: true ) {
                borderLayout()
                scrollPane( constraints: BorderLayout.CENTER ) {
                    panel( name: 'main-panel' ) {
                        gridBagLayout()
                        label( 'Y! query:',
                                constraints: cnst( row: 1, col: 1 ) )
                        textArea( name: 'query-field', rows: 5, columns: 30,
                                constraints: cnst( row: 1, col: 2, fill: HORIZONTAL ) )
                        button( 'Run', name: 'run-button', preferredSize: [ 100, 25 ] as Dimension,
                                constraints: cnst( row: 1, col: 3 ) )
                        label( name: 'status-label',
                                constraints: cnst( row: 1, col: 4, fill: HORIZONTAL ) )
                        label( name: 'current-weather',
                                constraints: cnst( row: 1, col: 5, fill: HORIZONTAL ) )
                        def t1 = table( name: 'forecast-table',
                                constraints: cnst( row: 1, col: 7, ipady: 4, fill: BOTH ) ) {
                            tableModel( list: forecastData ) {
                                tableColumns( [
                                        [ name: 'Date', property: 'date' ],
                                        [ name: 'Low', property: 'low' ],
                                        [ name: 'High', property: 'high' ],
                                        [ name: 'Conditions', property: 'text' ]
                                ] )
                            }
                        }
                        widget( constraints: cnst( row: 1, col: 6, fill: HORIZONTAL ), t1.tableHeader )
                    }
                }
            }
            consumeNextAction jframe, nextActions
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
                status.foreground = defaultForeground
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