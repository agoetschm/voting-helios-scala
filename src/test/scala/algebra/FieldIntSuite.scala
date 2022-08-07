package algebra

class FieldIntSuite extends munit.FunSuite:
  test("multiplication") {
    val field = FieldInt(11)
    assertEquals(field(3) * field(5), field(4))
  }
  test("addition") {
    val field = FieldInt(11)
    assertEquals(field(9) + field(2), field(11))
    assertEquals(field(4) + field(-6), field(9))
  }
