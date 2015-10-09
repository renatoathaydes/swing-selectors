package com.athaydes.automaton.swing.selectors

import groovy.swing.SwingBuilder
import spock.lang.Specification
import spock.lang.Subject

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JWindow
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.tree.TreeNode
import java.awt.Dimension
import java.awt.Point
import java.awt.Window

class SwingSelectorTest extends Specification {

    JFrame jframe = new SwingBuilder().frame( show: false, name: 'FRAME' ) {
        def tModel = [ [ firstCol: 'item 1 - Col 1', secCol: 'item 1 - Col 2' ] ]
        dialog( show: false, name: 'The dialog' ) {
            vbox {
                label( 'This is a dialog' )
                textField( 'write something...' )
            }
        }
        scrollPane {
            table {
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
                label( 'A window', name: 'label-in-a-window' )
                textArea( 'Write text here.' )
                textArea( 'write something...' )
            }
        }
    }

    @Subject
    def selector = new SwingSelector( root: jframe )

    @SuppressWarnings( "GroovyAssignabilityCheck" )
    def "Can select an element by type"() {
        given: 'The large UI used by this test'

        when: 'Trying to select an existing component by type'
        def result = selector.selectWithType( type )

        then: 'The selected component meets the expected conditions'
        type.isInstance( result )
        assertion.call( result )

        where:
        type      | assertion
        JFrame    | { JFrame frame -> frame?.name == 'FRAME' }
        JDialog   | { JDialog dialog -> dialog?.name == 'The dialog' }
        JTextArea | { JTextArea ta -> ta?.text == 'Write text here.' }
        TreeNode  | { TreeNode tn -> tn?.toString() == 'JTree' }
    }

    def "Can select all elements by type"() {
        given: 'The large UI used by this test'

        when: 'Trying to select existing components by type'
        def result = selector.selectAllWithType( type )

        then: 'The selected component meets the expected conditions'
        result.every { type.isInstance( it ) }
        assertion.call( result )

        where:
        type              | assertion
        JFrame            | { List frames -> frames?.size() == 1 }
        JDialog           | { List dialogs -> dialogs?.size() == 1 }
        JTextArea         | { List tas -> tas?.size() == 2 }
        TableCellRenderer | { List tcrs -> tcrs?.size() == 2 }
    }

    def "Can select all elements by name"() {
        given: 'The large UI used by this test'

        when: 'Trying to select existing components by name'
        def result = selector.selectAllWithName( name )

        then: 'The selected component meets the expected conditions'
        assertion.call( result )

        where:
        name                | assertion
        'FRAME'             | { List list -> list?.size() == 1 && list.first() instanceof JFrame }
        'The dialog'        | { List list -> list?.size() == 1 && list.first() instanceof JDialog }
        'some window'       | { List list -> list?.size() == 1 && list.first() instanceof Window }
        'label-in-a-window' | { List list -> list?.size() == 1 && list.first() instanceof JLabel }
    }

    def "Can select all elements by text"() {
        given: 'The large UI used by this test'

        when: 'Trying to select existing components by text'
        def result = selector.selectAllWithText( text )

        then: 'The selected component meets the expected conditions'
        assertion.call( result )

        where:
        text                 | assertion
        'Col 1'              | { List list -> list?.size() == 1 && list.first() instanceof TableColumn }
        'item 1 - Col 2'     | { List list -> list?.size() == 1 && list.first() instanceof JLabel }
        'red'                | { List list -> list?.size() == 1 && list.first() instanceof TreeNode }
        'A window'           | { List list -> list?.size() == 1 && list.first() instanceof JLabel }
        'write something...' | { List list -> list?.size() == 2 && list.any { it instanceof JTextField } }
        'write something...' | { List list -> list?.size() == 2 && list.any { it instanceof JTextArea } }
    }

    def "Can select all elements meeting some predicate"() {
        given: 'The large UI used by this test'

        when: 'Trying to select existing components by predicate'
        def result = selector.selectAll( predicate )

        then: 'The number of items selected is as expected'
        result.size() == count

        where:
        count || predicate
        2     || { it instanceof TableColumn }
        5     || { it instanceof JLabel }
        1     || { it instanceof JWindow }
        3     || { it instanceof JTextField || it instanceof JTextArea }
    }

    def "Trying to select items meeting impossible conditions always results in null"() {
        given: 'The large UI used by this test'

        when: 'Trying to select an existing components with a impossible predicate'
        def result = selector.select( predicate )

        then: 'Should always return null'
        result == null

        where:
        predicate << [
                { it instanceof Boolean },
                { it instanceof Number },
                { it == null }
        ]
    }


    def "Stops navigating as soon as the visitor returns true"() {
        given: 'The large UI used by this test'

        when: 'Selecting all items up to an instance of JFrame being met'
        def result = selector.selectAll( 100 ) { item ->
            item instanceof JFrame
        }

        then: 'All items visited up to the JFrame are returned'
        result.size() == 1
        result.first() instanceof JFrame
    }

    def "Stops navigating as soon as the limit is reached"() {
        given: 'The large UI used by this test'

        when: 'Selecting up to a certain number of items unconditionally'
        def result = selector.selectAll( limit ) { true }

        then: 'Only the limit number of items should have been visited'
        result.size() == limit

        where:
        limit << [ 0, 1, 2, 10 ]
    }
}
