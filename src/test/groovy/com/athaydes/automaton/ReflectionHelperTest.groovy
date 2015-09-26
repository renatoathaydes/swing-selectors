package com.athaydes.automaton

import spock.lang.Specification

class ReflectionHelperTest extends Specification {

    def "callMethodIfExists() should work with many different types, arguments, return values"() {
        when: 'Calling callMethodIfExists() on Object #obj, #methodName() and args #args'
        def result = ReflectionHelper.callMethodIfExists( obj, methodName, args )

        then: 'The expected result is returned'
        result == expected

        where:
        obj             | methodName          | args            | expected
        'hi'            | 'toUpperCase'       | [ ] as Object[] | 'HI'
        'hi'            | 'nonExistentMethod' | [ ] as Object[] | [ ]
        [ 1 ]           | 'add'               | 2               | true
        [ 1, 2 ]        | 'addAll'            | [ 3, 4 ]        | true
        [ 1, 2 ] as Set | 'addAll'            | [ 1, 2 ]        | false
    }

    def "callMethodIfExists() call should fail when the number of arguments is wrong"() {
        when: 'Calling callMethodIfExists() on Object #obj, #methodName() and args #args'
        def result = ReflectionHelper.callMethodIfExists( obj, methodName, args )

        then: 'The result should be the empty result'
        result == [ ]

        where:
        obj   | methodName       | args
        'hi'  | 'toUpperCase'    | [ 'arg' ] as Object[]
        [ 1 ] | 'ensureCapacity' | [ 'yes', 'no' ]
        'hi'  | 'charAt'         | [ '1' ]
    }

}
