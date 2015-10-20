# Swing Selectors

Swing Selectors makes it possible to declaratively select Swing components anywhere
in the Component tree.

For example:

```groovy
final selector = new SwingSelector( root: frame )
selector.selectAllWithType( JLabel ).each { label ->
    label.foreground = defaultForeground
}
```

This will set the foreground of all `JLabel`s under `frame`
(any Swing component, such as a `JFrame`) to a `defaultForeground` Color.

> Check out my blog [post about swing-selectors](https://sites.google.com/a/athaydes.com/renato-athaydes/posts/usingswing-selectorstocreatebeautifuluiswithgroovy)
for a nice sample usage.

## Requirements

> Swing-Selectors is hosted on [JCenter](https://bintray.com/bintray/jcenter),
so you must add the jcenter repository to your project.

Add a dependency on swing-selectors:

#### Gradle

```groovy
compile 'com.athaydes.automaton:swing-selectors:1.0'
```

#### Maven

```xml
<dependency>
  <groupId>com.athaydes.automaton</groupId>
  <artifactId>swing-selectors</artifactId>
  <version>1.0</version>
</dependency>
```

## Selecting multiple items

The `SwingSelector` class has the following methods to select multiple items:

* `List selectAll( Closure visitor )`
* `List selectAll( int limit, Closure visitor )`
* `List<T> selectAllWithType( Class<T> type, int limit = Integer.MAX_VALUE )`
* `List selectAllWithName( String name, int limit = Integer.MAX_VALUE )`
* `List selectAllWithText( String text, int limit = Integer.MAX_VALUE )`

Examples:

```groovy
// get all JLabels
List<JLabel> labels = selector.selectAllWithType( JLabel )

// get all components that have the name 'a-component'
List components = selector.selectAllWithName( 'a-component' )

// get up to 10 components whose text values are 'File'
List fileComponents = selector.selectAllWithText( 'File', 10 )

// get all instances of JTextField whose text is empty
List emptyFields = selector.selectAll { it instanceof JTextField && !it.text }
```

## Selecting a single item

The following methods can be used to select single items:

* `Object select( Closure visitor )`
* `Object selectWithType( Class<T> type )`
* `Object selectWithName( String name )`
* `Object selectWithText( String text )`

All methods return null if an item cannot be found.

Examples:

```groovy
// get a JDialog
JDialog popup = selector.selectWithType( JDialog )

// get a component with name 'a-component'
def component = selector.selectWithName( 'a-component' )

// get a component whose text value is 'File'
def fileComponent = selector.selectWithText( 'File' )

// get a JTextField whose name is 'input-field'
def inputField = selector.select { it instanceof JTextField && it.name == 'input-field' }
```

## SwingSelector cache

By default, `SwingSelector` uses a cache to make finding components faster
(one order of magnitude faster in the [performance tests]
(src/test/groovy/com/athaydes/automaton/swing/selectors/PerformanceTest.groovy)
I performed).

For this reason, finding all components (slowest operation) matching a predicate
usually takes just a few milli-seconds. The tests, which use a very large UI with
over 7000 components, show that it might take less than 5 ms to find all 4343 instances
of `JLabel`, for example.

However, caching may be a problem for highly dynamic UIs.

To manage this problem, you can manually update the cache by revalidating it:

```groovy
def selector = new SwingSelector( root: frame )

// UI changes take place...

selector.revalidateCache()

// caching happens in a separate Thread...
// so if you want to block until the cache is ready you must explicitly do it
boolean done = selector.revalidateCache().await( 2, TimeUnit.SECONDS )
if (!done) {
    println "Cache not ready yet!"
}
```

To completely turn off caching, build `SwingSelector` as follows:

```groovy
def selector = new SwingSelector( root: frame, useCache: false )
```

## Navigating through the component tree

The `SwingNavigator` class has a few methods for navigating the Swing component tree, the main one
being `navigateBreadthFirst`. It takes a visitor which receives each item
of the tree. It may return `true` to stop navigation.

For example, to navigate a tree, printing each Component:

```groovy
SwingNavigator.navigateBreadthFirst( frame ) { component ->
    println component
}
```

A more practical example - set the Font of every component which has
a `setFont` method to our default font:

```groovy
final defaultFont = new Font( 'Arial', Font.PLAIN, 12 )

SwingNavigator.navigateBreadthFirst( frame ) { component ->
    ReflectionHelper.callMethodIfExists( component,
        "setFont", defaultFont )
}
```

> To do this, the `ReflectionHelper` class is used, which is also part of the
  `swing-selectors` library and comes very handy in cases like this, where we want
  to perform an operation on all components which support it but there's no single
  type we can select to do it

The public methods of `SwingNavigator` are:

* `boolean navigateBreadthFirst( Object root, Closure visitor )`
* `boolean visitTable( JTable table, Closure visitor )`
* `boolean visitTree( JTree tree, Closure visitor )`
* `boolean visitTreeNode( TreeNode node, TreeModel model, Closure visitor )`

All methods return whatever the visitor returned for the last visited component.

`navigateBreadthFirst` delegates to the appropriate method listed above when meeting a
component of `JTable`, `JTree` or `TreeNode` type.
When a `JDialog` or `JWindow` is visited, all of its sub-tree is visited breadth-first before
moving on.

When visiting a `JTable`, the visitor may optionally take 2 more arguments: `row` and `column`, both
of type `Integer`.
The first cells visited are actually the headers (represented by the table's `TableColumn`s),
 which have a row index of `-1`. Then,
the rows in the first column (index starting with 0) are visited, then the rows in the next
column and so on. The cells visited in a `JTable` are actually the cell's renderer components.

Example showing how to navigate a JTable:

```groovy
JTable table = selector.selectWithType( JTable )

SwingNavigator.visitTable( table ) { component, row, col ->
    println "Row $row, Column $col -> $component"
}
```

Sample output:

```
Row -1, Column 0 -> groovy.model.DefaultTableColumn@3fc44af4[header:Col 1 valueModel:groovy.model.PropertyModel@7014f9e0]
Row -1, Column 1 -> groovy.model.DefaultTableColumn@71211a9c[header:Col 2 valueModel:groovy.model.PropertyModel@345a31ca]
Row 0, Column 0 -> javax.swing.table.DefaultTableCellRenderer$UIResource[Table.cellRenderer,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.0,border=javax.swing.plaf.BorderUIResource$LineBorderUIResource@d7adfa0,flags=25165832,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,horizontalAlignment=LEADING,horizontalTextPosition=TRAILING,iconTextGap=4,labelFor=,text=item 1 - Col 1,verticalAlignment=CENTER,verticalTextPosition=CENTER]
Row 0, Column 1 -> javax.swing.table.DefaultTableCellRenderer$UIResource[Table.cellRenderer,0,0,0x0,invalid,alignmentX=0.0,alignmentY=0.0,border=javax.swing.plaf.BorderUIResource$LineBorderUIResource@d7adfa0,flags=25165832,maximumSize=,minimumSize=,preferredSize=,defaultIcon=,disabledIcon=,horizontalAlignment=LEADING,horizontalTextPosition=TRAILING,iconTextGap=4,labelFor=,text=item 1 - Col 2,verticalAlignment=CENTER,verticalTextPosition=CENTER]
```
