package algebra

import scala.annotation.tailrec

object AlgebraIntImpl:
  def mult(a: Int, b: Int): Int = a * b
  def inverse(a: Int, modulo: Int): Int =
    ExtendedEuclideanAlgo(a, modulo)._1
  def add(a: Int, b: Int): Int = a + b
  def negation(a: Int): Int    = -a
  def nonNegativeModulo(a: Int, modulo: Int): Int =
    ((a % modulo) + modulo) % modulo // making sure it's >= 0
  def modularExponentation(base: Int, exponent: Int, modulo: Int): Int =
    @tailrec def power(acc: Int, exponentLeftover: Int): Int =
      if (exponentLeftover < 1)
        acc
      else
        power((acc * base) % modulo, exponentLeftover - 1)
    power(1, exponent)
