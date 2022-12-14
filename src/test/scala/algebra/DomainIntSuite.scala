package algebra

class DomainIntSuite extends munit.FunSuite:
  test("exponent is reducible modulo q") {
    val domain = DomainInt(107, 53, 75)
    val g      = domain.generator
    val exp    = domain.exponent

    assertEquals(g(exp(5) * exp(15)), g(exp(5)) ^ exp(15))
  }
