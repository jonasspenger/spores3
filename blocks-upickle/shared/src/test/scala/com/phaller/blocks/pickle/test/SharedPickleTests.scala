package com.phaller.blocks.pickle.test

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.phaller.blocks.{Block, Creator, CBlock}

import upickle.default._


@RunWith(classOf[JUnit4])
class SharedPickleTests {

  @Test
  def test1(): Unit = {
    val res = write(Seq(1, 2, 3))
    assert(res == "[1,2,3]")
  }

  @Test
  def testCreator(): Unit = {
    val c = Creator[Int, Int, Int]("com.phaller.blocks.pickle.test.MyBlock")
    val s = c(12)
    val res = s(3)
    assert(res == 16)
  }

  type BlockT = Block[Int, Int] { type Env = Int }

  @Test
  def testBlockReadWriter(): Unit = {
    val name = "com.phaller.blocks.pickle.test.MyBlock"
    val env: Int = 12

    given blockReadWriter: ReadWriter[BlockT] =
      readwriter[ujson.Value].bimap[BlockT](
        // currently, cannot obtain creator name from block `b`
        b => ujson.Arr(name, b.envir),
        json => {
          val n: String = json(0).str
          val i: Int = json(1).num.toInt
          val c = Creator[Int, Int, Int](n)
          c(i)
        }
      )

    // create a block
    val block: BlockT = MyBlock(12)

    // pickle block
    val res = write(block)

    assert(res == """["com.phaller.blocks.pickle.test.MyBlock",12]""")
  }

  @Test
  def testCBlockReadWriter(): Unit = {
    given blockReadWriter: ReadWriter[CBlock[Int, Int, Int]] =
      readwriter[ujson.Value].bimap[CBlock[Int, Int, Int]](
        cblock => ujson.Arr(cblock.creatorName, cblock.block.envir),
        json => {
          val n = json(0).str
          val i = json(1).num.toInt
          val c = Creator[Int, Int, Int](n)
          val cblock = c(i)
          new CBlock(n, cblock)
        }
      )

    // name of creator
    val name = "com.phaller.blocks.pickle.test.MyBlock"

    // create a CBlock
    val x = CBlock(12)[Int, Int](name)

    val res = write(x)

    assert(res == """["com.phaller.blocks.pickle.test.MyBlock",12]""")
  }

  @Test
  def testGenericCBlockReadWriter(): Unit = {
    given blockReadWriter[T, R]: ReadWriter[CBlock[Int, T, R]] =
      readwriter[ujson.Value].bimap[CBlock[Int, T, R]](
        cblock => ujson.Arr(cblock.creatorName, cblock.block.envir),
        json => {
          val n = json(0).str
          val i = json(1).num.toInt
          val c = Creator[Int, T, R](n)
          val cblock = c(i)
          new CBlock(n, cblock)
        }
      )

    // name of creator
    val name = "com.phaller.blocks.pickle.test.MyBlock"

    // create a CBlock
    val x = CBlock(12)[Int, Int](name)

    val res = write(x)

    assert(res == """["com.phaller.blocks.pickle.test.MyBlock",12]""")
  }

  @Test
  def testFullyGenericCBlockReadWriter(): Unit = {
    given blockReadWriter[E, T, R](using envReadWriter: ReadWriter[E]): ReadWriter[CBlock[E, T, R]] =
      readwriter[ujson.Value].bimap[CBlock[E, T, R]](
        cblock => {
          val pickledEnv = write(cblock.block.envir)
          ujson.Arr(cblock.creatorName, pickledEnv)
        },
        json => {
          val creatorName = json(0).str
          val env = read[E](json(1).str)
          val c = Creator[E, T, R](creatorName)
          val block = c(env)
          new CBlock(creatorName, block)
        }
      )

    // FQN of creator
    val name = "com.phaller.blocks.pickle.test.MyBlock"

    // create a CBlock
    val x = CBlock(12)[Int, Int](name)

    // pickle CBlock
    val res = write(x)

    assert(res == """["com.phaller.blocks.pickle.test.MyBlock","12"]""")

    val y = read[CBlock[Int, Int, Int]](res)
    val res2 = y(3)
    assert(res2 == 16)
  }

}
