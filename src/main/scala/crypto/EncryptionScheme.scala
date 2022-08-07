package crypto

/** Encryption scheme using a pair of public and private keys, for encryption and decryption
  * respectively, and a source of randomness for key generation as well as encryption.
  * @tparam PT
  *   plaintext
  * @tparam CT
  *   ciphertext
  * @tparam PK
  *   public key
  * @tparam SK
  *   secret key
  * @tparam D
  *   domain
  * @tparam R
  *   randomness
  */
trait EncryptionScheme[PT, CT, PK, SK, D, R]:
  val gen: (D, () => R) => (PK, SK)
  val enc: (PT, PK, () => R) => CT
  val dec: (CT, SK) => PT
