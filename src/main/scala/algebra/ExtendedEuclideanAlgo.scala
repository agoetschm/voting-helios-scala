package algebra

import scala.annotation.tailrec

// solves Bezout's identity: ax + by = gcd(a,b)
// input: a, b
// output: x, y
// https://en.wikipedia.org/wiki/Extended_Euclidean_algorithm
object ExtendedEuclideanAlgo {
  private case class Iteration(r1: Int, r2: Int, x1: Int, x2: Int, y1: Int, y2: Int)

  @tailrec
  private def eea(i: Iteration): (Int, Int) =
    if (i.r2 == 0) (i.x1, i.y1)
    else
      val d = i.r1 / i.r2
      val next =
        Iteration(
          r1 = i.r2,
          r2 = i.r1 - d * i.r2,
          x1 = i.x2,
          x2 = i.x1 - d * i.x2,
          y1 = i.y2,
          y2 = i.y1 - d * i.y2
        )
      eea(next)

  def apply(a: Int, b: Int): (Int, Int) =
    val init = Iteration(a, b, 1, 0, 0, 1)
    eea(init)
}
