package com.athaydes.automaton.swing.selectors

import groovy.transform.CompileStatic

import javax.swing.JTable
import javax.swing.JTree
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import java.awt.Component
import java.awt.Window

import static com.athaydes.automaton.ReflectionHelper.callMethodIfExists

/**
 * Methods to navigate through a Swing component tree.
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
        Collection nextLevel = [ root ]
        while ( nextLevel ) {
            if ( visit( nextLevel, action ) ) return true
            nextLevel = nextLevel.collectMany { c -> subItemsOf( c ) }.flatten()
        }
        return false
    }

    /**
     * Visits the nodes of the given tree, calling the given action for each node, including the root.
     * To stop navigating, action may return true
     * @param tree to be navigated
     * @param action to be called on each visited node. Return true to stop navigating.
     * @return true if action returned true for any node
     */
    static boolean visitTree( JTree tree, Closure action ) {
        visitTreeNode( tree.model.root as TreeNode, tree.model, action )
    }

    /**
     * Visits the given TreeNode branch, calling the given action for each node, including the startNode.
     * To stop navigating, action may return true
     * @param startNode node to start navigation from
     * @param model JTree model
     * @param action to be called on each visited node. Return true to stop navigating.
     * @return true if action returned true for any node
     */
    static boolean visitTreeNode( TreeNode startNode, TreeModel model, Closure action ) {
        if ( model ) {
            def nextLevel = [ startNode ]
            while ( nextLevel ) {
                if ( visit( nextLevel, action ) ) return true
                nextLevel = nextLevel.collectMany { node ->
                    node.children().toList()
                }.flatten()
            }
        }
        return false
    }

    /**
     * Visits the given table, calling the given action for each header and cell.
     *
     * The visited components are:
     * <ul>
     *     <li>headers: the actual TableColumns (row index is -1)</li>
     *     <li>cells: the TableCellRendererComponent of each cell (row and column indexes start at 0)</li>
     * </ul>
     *
     * To stop navigating, action may return true
     * @param table to navigate through
     * @param action to be called on each visited header/cell. Return true to stop navigating.
     * May take 1 argument (the cell being visited), 2, or 3 (row, column).
     * @return true if action returned true for any header/cell
     */
    static boolean visitTable( JTable table, Closure action ) {
        def invokeActionWithMax3Args = { item, row, col ->
            switch ( action.maximumNumberOfParameters ) {
                case 1: return action.call( item )
                case 2: return action.call( item, row )
                case 3: return action.call( item, row, col )
                default: throw new IllegalArgumentException( 'Action must take 1 to 3 arguments' )
            }
        }

        def cols = ( 0..<table.model.columnCount )

        if ( cols.any { int col ->
            invokeActionWithMax3Args( table.columnModel.getColumn( col ), -1, col )
        } ) {
            return true
        }

        def rows = ( 0..<table.model.rowCount )
        return [ rows, cols ].combinations().any { int row, int col ->
            invokeActionWithMax3Args(
                    getTableCellRendererComponent( table, row, col ),
                    row, col )
        }
    }

    private static Component getTableCellRendererComponent( JTable table, int row, int col ) {
        def value = table.model.getValueAt( row, col )
        table.getCellRenderer( row, col )
                .getTableCellRendererComponent( table, value, true, true, row, col )
    }

    private static Collection subItemsOf( component ) {
        def components = callMethodIfExists( component, 'getComponents' ) as List
        def menuBar = callMethodIfExists( component, 'getJMenuBar' )
        def menuComponents = callMethodIfExists( component, 'getMenuComponents' ) as List
        return components + ( menuBar ?: [ ] ) + menuComponents
    }

    private static visit( List nextLevel, Closure action ) {
        for ( item in nextLevel ) {
            if ( action( item ) ) return true
            switch ( item ) {
                case JTree:
                    if ( visitTree( item as JTree, action ) )
                        return true
                    break
                case JTable:
                    if ( visitTable( item as JTable, action ) )
                        return true
                    break
                case Window:
                    if ( ( item as Window ).ownedWindows.any { subWindow ->
                        navigateBreadthFirst( subWindow, action )
                    } ) return true
            }
        }
        return false
    }

}
