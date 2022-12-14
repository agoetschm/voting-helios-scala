package crypto

// maybe use the implementation from shapeless
// https://github.com/milessabin/shapeless/blob/654fe58518a9129b82a2b99b8beeed74504f75b0/core/src/main/scala/shapeless/sized.scala
case class SizedSeq[N <: Nat, T](size: N, underlying: Seq[T]):
  require(underlying.size == size.n)
  def map[B](f: T => B): SizedSeq[N, B] =
    SizedSeq(size, underlying.map(f))
  def zip[T2](other: SizedSeq[N, T2]): SizedSeq[N, (T, T2)] =
    SizedSeq(size, underlying.zip(other.underlying))
  def zipWithIndex: SizedSeq[N, (T, Int)] =
    SizedSeq(size, underlying.zipWithIndex)
  def reduce[B >: T](f: (T, T) => T): B     = underlying.reduce(f)
  def forall(p: T => Boolean): Boolean      = underlying.forall(p)
  def foldLeft[A](z: A)(op: (A, T) => A): A = underlying.foldLeft(z)(op)

object SizedSeq:
  def fill[N <: Nat, T](size: N, elem: T) = SizedSeq(size, Seq.fill(size.n)(elem))
