package algebra

class GroupIntSuite extends munit.FunSuite:
  test("order") {
    val group = GroupInt(23)
    assertEquals(group.order, 22)
  }
  test("multiplication") {
    val group = GroupInt(23)
    assertEquals(group(12) * group(5), group(14))
  }
  test("inverse") {
    val group = GroupInt(7)
    assertEquals(group(5).inv, group(3))
    assertEquals(group(5).inv * group(5), group(1))
  }
  test("exponentiation") {
    val group = GroupInt(23)
    val field = FieldInt(11)
    assertEquals(group(12) ^ field(2), group(6))

    val x      = GroupInt(11)(5)
    val field2 = FieldInt(5)
    assertEquals((x ^ field2(0)).z, 1)
    assertEquals((x ^ field2(1)).z, 5)
    assertEquals((x ^ field2(2)).z, 3)
    assertEquals((x ^ field2(3)).z, 4)
    assertEquals((x ^ field2(4)).z, 9)
    assertEquals((x ^ field2(5)).z, 1)

    assertEquals(((x ^ field2(2)) ^ field2(2)).z, 9)
    assertEquals((x ^ (field2(2) * field2(2))).z, 9)
    assertEquals((x ^ (field2(2) + field2(2))).z, 9)
  }
