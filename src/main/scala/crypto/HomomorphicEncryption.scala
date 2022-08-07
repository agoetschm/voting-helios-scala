package crypto

trait HomomorphicEncryption[C]:
  val combine: (C, C) => C
