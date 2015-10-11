package com.athaydes.automaton.swing.selectors

import groovy.swing.SwingBuilder
import groovy.transform.CompileStatic
import org.junit.Test

import javax.swing.JDialog
import javax.swing.JLabel
import java.awt.Dimension
import java.awt.Point
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class PerformanceTest {

    def frame = new SwingBuilder().frame( title: 'Frame', size: [ 600, 600 ] as Dimension,
            location: [ 150, 50 ] as Point, show: false ) {

        def tModel = [
                [ firstCol: 'abc', secCol: 'def', thirdCol: 'ghi', fourthCol: 'ijk' ]
        ] * 1000

        def deep = { Closure contents ->
            hbox {
                vbox {
                    label 'A scroll pane below me'
                    scrollPane {
                        hbox {
                            label 'this is a label'
                            textField()
                            panel {
                                panel {
                                    vbox {
                                        contents()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        menuBar {
            [ 'File', 'Edit', 'View', 'Code', 'Analyze', 'Refactor', 'Tools', 'Window', 'Help' ].eachWithIndex { m, i ->
                menu( name: "menu-button$i", text: m ) {
                    [ 'Exit', 'Add', 'Sub', 'Do', 'Blah', 'Find', 'Quit' ].eachWithIndex { n, j ->
                        menuItem( name: "menu-item-$j", text: n )
                    }
                }
            }
        }
        dialog( size: [ 150, 150 ] as Dimension,
                location: [ 250, 150 ] as Point,
                title: 'Dialog',
                show: false ) {
            vbox {
                30.times {
                    label( 'This is a dialog' )
                    textField( 'write something...' )
                }
            }
        }
        deep {
            scrollPane {
                table {
                    tableModel( list: tModel ) {
                        propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
                        propertyColumn( header: 'Col 2', propertyName: 'secCol' )
                        propertyColumn( header: 'Col 3', propertyName: 'thirdCol' )
                        propertyColumn( header: 'Col 4', propertyName: 'fourthCol' )
                    }
                }
            }
        }

        50.times {
            scrollPane {
                tree( rootVisible: false )
            }
        }

        window( show: false, name: 'some window',
                location: [ 350, 250 ] as Point,
                size: [ 150, 250 ] as Dimension ) {
            vbox {
                label( 'A window' )
                textArea( 'Write text here.' )
                panel {
                    10.times {
                        hbox {
                            deep {
                                label 'Very deep'
                                10.times { textField() }
                                5.times {
                                    deep {
                                        label 'Insane deep'
                                        10.times {
                                            button 'Click here'
                                        }
                                        label 'one more button'
                                        button 'Lonely button', name: 'lonely'
                                    }
                                }
                            }
                            hbox {
                                label 'hello'
                                label 'This is a hbox'
                                textField()
                                label 'Text Area'
                                textArea()
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void "Verify speed of selectors when trying to find UI items"() {
        def selector = new SwingSelector( root: frame, useCache: true )
        assert selector.revalidateCache().await( 2, TimeUnit.SECONDS )

        println "Running tests"

        3.times {
            def lonelyBtn = withTimer( 'find lonely button by text' ) {
                selector.selectWithText( 'Lonely button' )
            }
            assert lonelyBtn
        }

        3.times {
            def lonelyBtn = withTimer( 'find lonely button by name' ) {
                selector.selectWithName( 'lonely' )
            }
            assert lonelyBtn
        }

        3.times {
            def sports = withTimer( 'find all sports' ) {
                selector.selectAllWithText( 'sports' )
            }
            assert sports?.size() == 50
        }

        3.times {
            def dialogs = withTimer( 'find all dialogs' ) {
                selector.selectAllWithType( JDialog )
            }
            assert dialogs?.size() == 1
        }

        3.times {
            def labels = withTimer( 'find all labels' ) {
                selector.selectAllWithType( JLabel )
            }
            assert labels?.size() == 4343
        }
    }

    @CompileStatic
    static withTimer( String name, Callable action ) {
        long startTime = System.currentTimeMillis()
        try {
            action.call()
        } finally {
            println "$name took ${System.currentTimeMillis() - startTime} ms"
        }
    }

}
