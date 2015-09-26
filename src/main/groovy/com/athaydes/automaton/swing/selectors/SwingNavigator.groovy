package com.athaydes.automaton.swing.selectors

import groovy.transform.CompileStatic

import javax.swing.JTable
import javax.swing.JTree
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

import static com.athaydes.automaton.ReflectionHelper.callMethodIfExists

/**
 *
 */
@CompileStatic
class SwingNavigator {

    /**
     * Navigates the tree under the given root, calling the given action for each Component.
     * To stop navigating, action may return true
     * @param root of tree to be navigated
     * @param action to be called on each visited Component. Return true to stop navigating.
     * @return true if action returned true for any Component
     */
    static boolean navigateBreadthFirst( root, Closure action ) {
        List nextLevel = [ root ]
        while ( nextLevel ) {
            if ( visit( nextLevel, action ) ) return true
            nextLevel = nextLevel.collectMany { c -> subItemsOf( c ) }.flatten()
        }
        return false
    }

    /**
     * Navigates the given tree, calling the given action for each node, including the root.
     * To stop navigating, action may return true
     * @param tree to be navigated
     * @param action to be called on each visited node. Return true to stop navigating.
     * @return true if action returned true for any node
     */
    @CompileStatic
    static boolean navigateBreadthFirst( JTree tree, Closure action ) {
        navigateBreadthFirst( tree.model.root as TreeNode, tree.model, action )
    }

    /**
     * Navigates the given tree, calling the given action for each node, including the startNode.
     * To stop navigating, action may return true
     * @param startNode node to start navigation from
     * @param model JTree model
     * @param action to be called on each visited node. Return true to stop navigating.
     * @return true if action returned true for any node
     */
    @CompileStatic
    static boolean navigateBreadthFirst( TreeNode startNode, TreeModel model, Closure action ) {
        if ( model ) {
            def nextLevel = [ startNode ]
            while ( nextLevel ) {
                if ( visit( nextLevel, action ) ) return true
                nextLevel = nextLevel.collect { node ->
                    ( 0..<model.getChildCount( node ) ).collect { int i ->
                        model.getChild( node, i )
                    }
                }.flatten()
            }
        }
        return false
    }

    /**
     * Navigates the given table, calling the given action for each header and cell.
     * To stop navigating, action may return true
     * @param table to navigate through
     * @param action to be called on each visited header/cell. Return true to stop navigating.
     * @return true if action returned true for any header/cell
     */
    @CompileStatic
    static boolean navigateBreadthFirst( JTable table, Closure action ) {
        def cols = ( 0..<table.model.columnCount )
        def rows = ( 0..<table.model.rowCount )
        for ( int col in cols ) {
            if ( action( table.model.getColumnName( col ), -1, col ) ) return true
        }
        for ( int row in rows ) {
            for ( int col in cols ) {
                if ( action( getRenderedTableValue( table, row, col ), row, col ) ) return true
            }
        }
        return false
    }

    /**
     * Returns the text as rendered by the table cell's renderer component, if the renderer component
     * has a getText() method. Returns the model value at the cell's position otherwise.
     * @param table in question
     * @param row of the value to get
     * @param col of the value to get
     * @return The rendered value or the model value if the renderer doesn't have a getText() method
     */
    @CompileStatic
    private static getRenderedTableValue( JTable table, int row, int col ) {
        def value = table.model.getValueAt( row, col )
        def rendererComp = table.getCellRenderer( row, col )
                .getTableCellRendererComponent( table, value, false, false, row, col )
        def text = callMethodIfExists( rendererComp, 'getText' )
        return text ?: value
    }

    @CompileStatic
    private static List subItemsOf( component ) {
        println "checking component $component"
        def contentPane = callMethodIfExists( component, 'getContentPane' )
        if ( contentPane ) {
            return subItemsOf( contentPane )
        }
        List components = callMethodIfExists( component, 'getComponents' ) as List
        List menuComponents = callMethodIfExists( component, 'getMenuComponents' ) as List

        List result = components + menuComponents
        println "Children of $component: $result"
        return result
    }

    @CompileStatic
    private static visit( List nextLevel, Closure action ) {
        for ( item in nextLevel ) if ( action( item ) ) return true
        return false
    }

}
