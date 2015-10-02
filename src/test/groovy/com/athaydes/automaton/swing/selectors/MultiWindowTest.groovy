package com.athaydes.automaton.swing.selectors

import groovy.swing.SwingBuilder
import spock.lang.Specification

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.tree.TreeNode
import java.awt.Dimension
import java.awt.Point
import java.awt.Window

class MultiWindowTest extends Specification {

    def "SwingNavigator should be able to navigate to components in any sub-window, table or tree"() {
        given: 'The table model for a JTable'
        def tModel = [
                [ firstCol: 'item 1 - Col 1', secCol: 'item 1 - Col 2' ],
                [ firstCol: 'item 2 - Col 1', secCol: 'item 2 - Col 2' ],
                [ firstCol: 'item 3 - Col 1', secCol: 'item 3 - Col 2' ]
        ]

        and: 'A large Swing UI with a JTree, a JTable and a few sub-windows'
        JTable jTable = null
        JFrame mainFrame = null
        new SwingBuilder().edt {
            mainFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension,
                    location: [ 150, 50 ] as Point, show: false ) {

                menuBar {
                    menu( name: 'menu-button', text: "File" ) {
                        menuItem( name: 'item-exit', text: "Exit" )
                    }
                }
                dialog( size: [ 150, 150 ] as Dimension,
                        location: [ 250, 150 ] as Point,
                        title: 'Dialog',
                        show: false ) {
                    vbox {
                        label( 'This is a dialog' )
                        textField( 'write something...' )
                    }
                }
                scrollPane {
                    jTable = table {
                        tableModel( list: tModel ) {
                            propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
                            propertyColumn( header: 'Col 2', propertyName: 'secCol' )
                        }
                    }
                }
                scrollPane {
                    tree( rootVisible: false )
                }

                window( show: false, name: 'some window',
                        location: [ 350, 250 ] as Point,
                        size: [ 150, 250 ] as Dimension ) {
                    vbox {
                        label( 'A window' )
                        textArea( 'Write text here.' )
                    }
                }
            }
        }

        when: 'We navigate through the whole main JFrame'
        def allComponents = [ ]
        SwingNavigator.navigateBreadthFirst( mainFrame ) { component ->
            // do not keep a reference to a cell renderer as it get re-used between cells
            allComponents << ( component instanceof DefaultTableCellRenderer ?
                    component.text :
                    component )
            return false
        }

        then: 'All windows should be visited'
        allComponents.any { it instanceof JFrame && it.title == 'Frame' }
        allComponents.any { it instanceof JDialog && it.title == 'Dialog' }
        allComponents.any { it instanceof Window && it.name == 'some window' }

        and: 'Components of each window were visited'
        allComponents.any { it instanceof JLabel && it.text == 'This is a dialog' }
        allComponents.any { it instanceof JLabel && it.text == 'A window' }
        allComponents.count { it instanceof JTable } == 1
        allComponents.count { it instanceof JTree } == 1

        and: 'The JTable headers and cells were visited'
        allComponents.any { it instanceof TableColumn && it.headerValue == 'Col 1' }
        allComponents.any { it instanceof TableColumn && it.headerValue == 'Col 2' }
        allComponents.any { it == 'item 1 - Col 1' }
        allComponents.any { it == 'item 1 - Col 2' }
        allComponents.any { it == 'item 2 - Col 1' }
        allComponents.any { it == 'item 2 - Col 2' }
        allComponents.any { it == 'item 3 - Col 1' }
        allComponents.any { it == 'item 3 - Col 2' }

        and: 'The JTree nodes were visited'
        allComponents.any { it instanceof TreeNode && it.toString() == 'red' }

        and: 'The MenuBar was visited'
        allComponents.any { it instanceof JMenuBar }
        allComponents.any { it instanceof JMenuItem && it.text == 'Exit' }
    }

}
