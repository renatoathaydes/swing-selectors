package com.athaydes.automaton.swing.selectors

import groovy.swing.SwingBuilder
import spock.lang.Specification

import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableColumn
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Point

class SwingNavigatorTest extends Specification {

    JFrame jFrame

    JFrame getJFrame() { jFrame }

    def "Can navigate breadth first every component of a realistic tree"() {
        given: 'A realistic tree of Swing components'
        def empty = { name ->
            Stub( Container ) {
                getComponents() >> Collections.emptyList()
                toString() >> ( name as String )
            }
        }
        def c1a = empty 'c1a'
        def c2_1a = empty 'c2_1a'
        def c2_1b = empty 'c2_1b'
        def c2cp1 = empty 'c2cp1'
        def c2cp = Stub( Container ) {
            getComponents() >> [ c2cp1 ]
            toString() >> 'c2cp'
        }
        def c2_1 = Stub( JMenu ) {
            getMenuComponents() >> [ c2_1a, c2_1b ]
            toString() >> 'c2_1'
        }
        def c1 = Stub( Container ) {
            getComponents() >> [ c1a ]
            toString() >> 'c1'
        }
        def c2 = Stub( JComponent ) {
            getComponents() >> [ c2_1, c2cp ]
            toString() >> 'c2'
        }
        def rootPane = Stub( JComponent ) {
            getComponents() >> [ c1, c2 ]
            toString() >> 'rootPane'
        }
        def root = Stub( JDialog ) {
            getContentPane() >> rootPane
            toString() >> 'root'
        }

        and: 'An action that remembers all components visited and always returns false'
        def visited = [ ]
        def action = { c -> visited += c; false }

        when: 'navigateBreadthFirst()'
        def res = SwingNavigator.navigateBreadthFirst root, action

        then: 'All tree components are visited'
        visited == [ root, c1, c2, c1a, c2_1, c2cp, c2_1a, c2_1b, c2cp1 ]

        and: 'The method returns false'
        !res
    }

    def "Can navigate a Component Tree only partially"() {
        given: 'A Simple tree of components'
        def empty = { name ->
            Stub( Container ) {
                getComponents() >> [ ]
                toString() >> ( name as String )
            }
        }
        def c1 = empty 'c1'
        def c2 = empty 'c2'
        def root = Stub( JComponent ) {
            getComponents() >> [ c1, c2 ]
            toString() >> 'root'
        }

        and: 'A visitor which stops as soon as it sees the c1 component'
        def visited = [ ]
        def action = { Component c -> visited += c; c.toString() == 'c1' }

        when: 'navigateBreadthFirst()'
        def res = SwingNavigator.navigateBreadthFirst root, action

        then: 'The navigation should have stopped at c1'
        visited == [ root, c1 ]

        and: 'The method should return true'
        res
    }

    def "Can visit a whole JTree of components"() {
        given: 'A real JFrame containing a JTree with the default components'
        JTree mTree = null
        new SwingBuilder().build {
            frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: false ) {
                mTree = tree( rootVisible: false )
            }
        }

        and: 'An action that remembers all components visited and always returns false'
        def visited = [ ]
        def action = { c -> visited += c; false }

        when: 'visitTree() starting with the JTree'
        def res = SwingNavigator.visitTree mTree, action

        then: 'All components of the JTree are visited'
        visited.collect { it as String } == [ mTree.model.root as String,
                                              'colors', 'sports', 'food',
                                              'blue', 'violet', 'red', 'yellow',
                                              'basketball', 'soccer', 'football', 'hockey',
                                              'hot dogs', 'pizza', 'ravioli', 'bananas' ]

        and: 'The method return false'
        !res
    }

    def "Can visit part of a JTree"() {
        given: 'A real JFrame with a JTree in it'
        JTree mTree = null
        new SwingBuilder().build {
            frame( title: 'Frame', size: [ 300, 300 ] as Dimension, show: false ) {
                mTree = tree( rootVisible: false )
            }
        }

        and: 'An action that remembers all components visited and returns false when visiting the blue component'
        def visited = [ ]
        def action = { c -> visited += c; c.toString() == 'blue' }

        when: 'visitTree() starting with the JTree'
        def res = SwingNavigator.visitTree mTree, action

        then: 'All components of the tree are visited until the blue component'
        visited.collect { it as String } == [ mTree.model.root as String,
                                              'colors', 'sports', 'food', 'blue' ]

        and: 'The method returns true'
        res
    }

    def "Can visit a JTable fully"() {
        given: 'The table model for a JTable'
        def tModel = [
                [ firstCol: 'item 1 - Col 1', secCol: 'item 1 - Col 2' ],
                [ firstCol: 'item 2 - Col 1', secCol: 'item 2 - Col 2' ],
                [ firstCol: 'item 3 - Col 1', secCol: 'item 3 - Col 2' ]
        ]

        and: 'A real JFrame containing a JTable using the model'
        JTable jTable = null
        new SwingBuilder().build {
            jFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension,
                    location: [ 150, 50 ] as Point, show: false ) {
                scrollPane {
                    jTable = table {
                        tableModel( list: tModel ) {
                            propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
                            propertyColumn( header: 'Col 2', propertyName: 'secCol' )
                        }
                    }
                }
            }
        }

        when: 'visitTable() starting on the JTable'
        def visited = [ ] as LinkedList<List>
        def res = SwingNavigator.visitTable( jTable ) { item, row, col ->
            if ( item instanceof DefaultTableCellRenderer ) {
                item = item.text // remember the text as the cell renderer is re-used amongst cells
            }
            visited << [ item, row, col ]
            false
        }

        then: 'The Table Headers should be visited'
        List firstVisit = visited.removeFirst()
        def header1 = firstVisit[ 0 ]
        header1 instanceof TableColumn && header1.headerValue == 'Col 1'
        firstVisit[ 1 ] == -1
        firstVisit[ 2 ] == 0

        List secondVisit = visited.removeFirst()
        def header2 = secondVisit[ 0 ]
        header2 instanceof TableColumn && header2.headerValue == 'Col 2'
        secondVisit[ 1 ] == -1
        secondVisit[ 2 ] == 1

        and: 'All cells of the JTable are visited and the visitor has access to row/column indexes'
        visited == [
                [ 'item 1 - Col 1', 0, 0 ],
                [ 'item 2 - Col 1', 1, 0 ],
                [ 'item 3 - Col 1', 2, 0 ],
                [ 'item 1 - Col 2', 0, 1 ],
                [ 'item 2 - Col 2', 1, 1 ],
                [ 'item 3 - Col 2', 2, 1 ]
        ] as LinkedList

        and: 'The method returns false'
        !res
    }

    def "Can visit JTable partially"() {
        given: 'The table model for a JTable'
        def tModel = [
                [ firstCol: 'item 1 - Col 1' ],
                [ firstCol: 'item 2 - Col 1' ],
        ]

        and: 'A real JFrame containing a JTable using the model'
        JTable jTable = null
        new SwingBuilder().build {
            jFrame = frame( title: 'Frame', size: [ 300, 300 ] as Dimension,
                    location: [ 150, 50 ] as Point, show: false ) {
                scrollPane {
                    jTable = table {
                        tableModel( list: tModel ) {
                            propertyColumn( header: 'Col 1', propertyName: 'firstCol' )
                        }
                    }
                }
            }
        }

        when: 'The cells of the JTable up to cell "item 1 - Col 1" are visited'
        LinkedList<List> visited = [ ]
        def res = SwingNavigator.visitTable( jTable ) { item, row, col ->
            if ( item instanceof DefaultTableCellRenderer ) {
                item = item.text // remember the text as the cell renderer is re-used amongst cells
            }
            visited << [ item, row, col ]
            item == 'item 1 - Col 1'
        }

        then: 'The Table Headers should be visited'
        List firstVisit = visited.removeFirst()
        def header1 = firstVisit[ 0 ]
        header1 instanceof TableColumn && header1.headerValue == 'Col 1'

        and: 'All cells of the JTable up to the last cell requested are visited'
        visited == [
                [ 'item 1 - Col 1', 0, 0 ],
        ] as LinkedList

        and: 'The method should return true'
        res
    }

}
