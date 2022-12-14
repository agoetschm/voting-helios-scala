package algebra

class ExtendedEuclidianAlgoSuite extends munit.FunSuite:
  test("solve Bezout's identity") {
    assertEquals(ExtendedEuclideanAlgo(6, 9), (-1, 1))
    assertEquals(ExtendedEuclideanAlgo(6, 13), (-2, 1))
  }
