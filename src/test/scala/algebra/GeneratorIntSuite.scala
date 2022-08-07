package algebra

class GeneratorIntSuite extends munit.FunSuite:
  test("check order of generator") {
    val group = GroupInt(23)
    val field = FieldInt(11)

    val g = GeneratorInt(group(12), field)
    assertEquals(Range(1, 11).map(i => g(field(i))).size, 10)
    intercept[IllegalArgumentException](GeneratorInt(group(1), field))
  }
