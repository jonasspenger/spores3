package com.phaller.spores.pickle.test

import scala.scalanative.reflect.annotation.EnableReflectiveInstantiation

import com.phaller.spores.Spore


@EnableReflectiveInstantiation
object MySpore extends Spore.Builder[Int, Int, Int](
  env => x => env + x + 1
)
