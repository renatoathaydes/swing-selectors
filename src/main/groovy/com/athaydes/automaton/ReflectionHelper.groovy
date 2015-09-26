package com.athaydes.automaton

import groovy.transform.CompileStatic

/**
 * Helper methods that assist with reflection.
 */
@CompileStatic
class ReflectionHelper {

    /**
     * Calls a method on the given Object with the given arguments
     * @param object to call method on
     * @param methodName to be called
     * @param args argument to be passed to the method
     * @return value returned by the method call, or the empty list if the method does not exist
     */
    static callMethodIfExists( object, String methodName, Object... args ) {
        def methods = object?.metaClass?.respondsTo( object, methodName, args )
        if ( methods ) {
            try {
                return methods.first().invoke( object, args )
            } catch ( ignored ) {
            }
        }
        return [ ]
    }

}
