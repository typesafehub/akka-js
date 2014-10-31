// AKKAJS OVERRIDES
/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.util

import annotation.tailrec

// import java.util.concurrent.{ ConcurrentSkipListSet, ConcurrentHashMap }
import java.util.Comparator
// import scala.collection.JavaConverters.{ asScalaIteratorConverter, collectionAsScalaIterableConverter }
import scala.collection.mutable

/**
 * An implementation of a ConcurrentMultiMap
 * Adds/remove is serialized over the specified key
 * Reads are fully concurrent <-- el-cheapo
 */
class Index[K, V](val mapSize: Int, val valueComparator: Comparator[V]) {

  def this(mapSize: Int, cmp: (V, V) ⇒ Int) = this(mapSize, new Comparator[V] {
    def compare(a: V, b: V): Int = cmp(a, b)
  })

  private val container = //new ConcurrentHashMap[K, ConcurrentSkipListSet[V]](mapSize)
    mutable.HashMap.empty[K, mutable.Set[V]]
  private val emptySet = //new ConcurrentSkipListSet[V]
    mutable.Set.empty[V]

  /**
   * Associates the value of type V with the key of type K
   * @return true if the value didn't exist for the key previously, and false otherwise
   */
  def put(key: K, value: V): Boolean = {
    val set = container.applyOrElse(key, (k: K) ⇒ {
      val newSet = mutable.Set.empty[V]
      container += (k -> newSet)
      newSet
    })
    set.add(value) // returns true if the element was not yet present in `set`
  }

  /**
   * @return Some(value) for the first matching value where the supplied function returns true for the given key,
   * if no matches it returns None
   */
  def findValue(key: K)(f: (V) ⇒ Boolean): Option[V] =
    container.applyOrElse(key, (k: K) ⇒ emptySet).find(f)

  /**
   * Returns an Iterator of V containing the values for the supplied key, or an empty iterator if the key doesn't exist
   */
  def valueIterator(key: K): scala.Iterator[V] = {
    container.applyOrElse(key, (k: K) ⇒ emptySet).iterator
  }

  /**
   * Applies the supplied function to all keys and their values
   */
  def foreach(fun: (K, V) ⇒ Unit): Unit =
    container foreach { entry ⇒ entry._2.foreach(v ⇒ fun(entry._1, v)) }

  /**
   * Returns the union of all value sets.
   */
  def values: Set[V] = {
    val builder = Set.newBuilder[V]
    for {
      values ← container.valuesIterator
      v ← values.iterator
    } builder += v
    builder.result()
  }

  /**
   * Returns the key set.
   */
  def keys: Iterable[K] = container.keys

  /**
   * Disassociates the value of type V from the key of type K
   * @return true if the value was disassociated from the key and false if it wasn't previously associated with the key
   */
  def remove(key: K, value: V): Boolean = {
    val set = container.applyOrElse(key, (k: K) ⇒ emptySet)
    val removed = set.remove(value)
    if (removed && set.isEmpty) container -= key
    removed
  }

  /**
   * Disassociates all the values for the specified key
   * @return None if the key wasn't associated at all, or Some(scala.Iterable[V]) if it was associated
   */
  def remove(key: K): Option[Iterable[V]] = container.get(key) match {
    case None ⇒ None
    case Some(set) ⇒
      val cloned = set.clone()
      set.clear()
      Some(cloned)
  }

  /**
   * Removes the specified value from all keys
   */
  def removeValue(value: V): Unit = {
    //TODO
    /*
    val i = container.entrySet().iterator()
    while (i.hasNext) {
      val e = i.next()
      val set = e.getValue()

      if (set ne null) {
        set.synchronized {
          if (set.remove(value)) { //If we can remove the value
            if (set.isEmpty) //and the set becomes empty
              container.remove(e.getKey, emptySet) //We try to remove the key if it's mapped to an empty set
          }
        }
      }
    }
*/
  }

  /**
   * @return true if the underlying containers is empty, may report false negatives when the last remove is underway
   */
  def isEmpty: Boolean = container.isEmpty

  /**
   *  Removes all keys and all values
   */
  def clear(): Unit = {
    // TODO
    /*
    val i = container.entrySet().iterator()
    while (i.hasNext) {
      val e = i.next()
      val set = e.getValue()
      if (set ne null) { set.synchronized { set.clear(); container.remove(e.getKey, emptySet) } }
    }
    */
  }
}

/**
 * An implementation of a ConcurrentMultiMap
 * Adds/remove is serialized over the specified key
 * Reads are fully concurrent <-- el-cheapo
 */
class ConcurrentMultiMap[K, V](mapSize: Int, valueComparator: Comparator[V]) extends Index[K, V](mapSize, valueComparator)
