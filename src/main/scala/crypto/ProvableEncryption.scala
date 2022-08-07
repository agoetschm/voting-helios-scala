package crypto

trait ProvableEncryption[PT, CT, DT, PK, SK, R, H, PoG, PoE, PoD]:
  val proveGen: (PK, SK, () => R, H) => PoG
  val verifyGen: (PK, PoG, H) => Boolean
  val proveEnc: (PT, PK, CT, () => R, H) => PoE
  val verifyEnc: (CT, PK, PoE, H) => Boolean
  val proveDec: (CT, SK, DT, () => R, H) => PoD
  val verifyDec: (CT, DT, PK, PoD, H) => Boolean
