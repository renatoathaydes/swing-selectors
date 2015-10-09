package com.athaydes.automaton.swing.selectors

import groovy.transform.Canonical
import groovy.transform.CompileStatic

import javax.swing.table.TableColumn
import javax.swing.tree.TreeNode

import static com.athaydes.automaton.ReflectionHelper.callMethodIfExists

/**
 * A Swing component selector.
 *
 * There are methods for selecting a single element (<code>selectWithX</code>)
 * and for selecting all elements (<code>selectAllWithX</code> matching a certain predicate.
 *
 * If a method to select a single element is used and the element is not found, the return value will be null.
 */
@CompileStatic
@Canonical
class SwingSelector {

    /**
     * The root element to search from
     */
    def root = null

    /**
     * Select an element with the given type.
     *
     * Notice that instances of subtypes of the given type are also selected.
     * @param type to select
     * @return instance of the given type if any, null otherwise.
     */
    def <T> T selectWithType( Class<T> type ) {
        firstOrNull selectAllWithType( type, 1 )
    }

    /**
     * Select an element with the given text.
     *
     * The text of an element is usually what is seen on the screen, not necessarily what its value may be.
     * @param text of element to be selected
     * @return element with the given text if any, null otherwise.
     */
    def selectWithText( String text ) {
        firstOrNull selectAllWithText( text, 1 )
    }

    /**
     * Select an element with the given name.
     *
     * @param name of element to be selected
     * @return element with the given name if any, null otherwise.
     */
    def selectWithName( String name ) {
        firstOrNull selectAllWithName( name, 1 )
    }

    /**
     * Selects a Swing item for which the given visitor returns Groovy truth.
     * @param visitor a closure which takes a Swing item as argument, returning an Object satisfying Groovy truth
     * if the Swing item is to be included in the result.
     * @return the selected Swing item or null if none is selected.
     */
    def select( Closure visitor ) {
        firstOrNull selectAll( 1, visitor )
    }

    /**
     * Select all elements with the given type.
     *
     * Notice that instances of subtypes of the given type are also selected.
     * @param type to select
     * @param limit maximum number of elements to return
     * @return all instances of the given type.
     */
    def <T> List<T> selectAllWithType( Class<T> type, int limit = Integer.MAX_VALUE ) {
        selectAll( limit ) { item -> type.isInstance( item ) } as List<T>
    }

    /**
     * Select all elements with the given text.
     *
     * The text of an element is usually what is seen on the screen, not necessarily what its value may be.
     * @param text of elements to be selected
     * @param limit maximum number of elements to return
     * @return all elements with the given text.
     */
    def List selectAllWithText( String text, int limit = Integer.MAX_VALUE ) {
        selectAll( limit ) { item ->
            switch ( item ) {
                case TableColumn: return ( item as TableColumn ).headerValue == text
                case TreeNode: return ( item as TreeNode ).toString() == text
                default: return callMethodIfExists( item, 'getText' ) == text
            }
        }
    }

    /**
     * Select all elements with the given name.
     *
     * @param name of elements to be selected
     * @param limit maximum number of elements to return
     * @return all elements with the given name.
     */
    def List selectAllWithName( String name, int limit = Integer.MAX_VALUE ) {
        selectAll( limit ) { item ->
            callMethodIfExists( item, 'getName' ) == name
        }
    }

    /**
     * Selects all Swing items for which the given visitor returns Groovy truth.
     * @param visitor a closure which takes a Swing item as argument, returning an Object satisfying Groovy truth
     * if the Swing item is to be included in the result.
     * @return List of all Swing items that have been selected.
     */
    List selectAll( Closure visitor ) {
        selectAll( Integer.MAX_VALUE, visitor )
    }

    /**
     * Selects all Swing items for which the given visitor returns Groovy truth.
     * @param limit maximum number of elements to return
     * @param visitor a closure which takes a Swing item as argument, returning an Object satisfying Groovy truth
     * if the Swing item is to be included in the result.
     * @return List of all Swing items that have been selected.
     */
    List selectAll( int limit, Closure visitor ) {
        List result = [ ]
        if ( limit < 1 ) {
            return result
        }
        SwingNavigator.navigateBreadthFirst( root ) { component ->
            if ( visitor.call( component ) ) {
                result << component
                return result.size() >= limit
            }
            return false
        }
        return result
    }

    /**
     * Returns the first element of the List if it is not empty, null otherwise.
     * @param list of elements
     * @return first element of the List or null.
     */
    static firstOrNull( List list ) {
        list.empty ? null : list.first()
    }

}
