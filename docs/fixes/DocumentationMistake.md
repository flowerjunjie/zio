# Documentation Fix: Immutable Data Mistake

## Issue #9350

### Current Documentation (Incorrect)
> Keep in mind that mutable data structures should not be used as immutable

### Corrected
Should state:
> Keep in mind that **immutable data structures should not be used as mutable**

### Context
This reverses the meaning. Immutable structures (like Scala collections) shouldn't be treated as mutable.

### Example
```scala
// WRONG - treating immutable as mutable
val list = List(1, 2, 3)
list.append(4) // Doesn't work!

// RIGHT - using immutable properly
val list = List(1, 2, 3)
val newList = list :+ 4 // Creates new instance
```

## Related
- Issue: #9350
