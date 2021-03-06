package com.stripe.rainier.sampler

final private case class WalkersChain(density: DensityFunction,
                                      walkers: Vector[Array[Double]],
                                      scores: Vector[Double],
                                      accepted: Boolean,
                                      walker: Int)(implicit rng: RNG) {

  val variables: Array[Double] = walkers(walker)
  val score: Double = scores(walker)

  def next: WalkersChain = {
    var target = rng.int(walkers.size)
    while (target == walker) target = rng.int(walkers.size)
    val (newWalker, newScore, newAccepted) = update(walkers(target))
    val nextWalker = (walker + 1) % walkers.size
    copy(walker = nextWalker,
         walkers = walkers.updated(walker, newWalker),
         scores = scores.updated(walker, newScore),
         accepted = newAccepted)
  }

  private def update(
      targetVariables: Array[Double]): (Array[Double], Double, Boolean) = {
    val z = Math.pow(rng.standardUniform + 1.0, 2) / 2.0

    val newVariables =
      variables.zip(targetVariables).map {
        case (from, to) =>
          to + (z * (from - to))
      }

    density.update(newVariables)
    val newScore = density.density
    val diff = (Math.log(z) * (variables.size - 1)) + newScore - score

    val accepted = Math.log(rng.standardUniform) < diff
    if (accepted)
      (newVariables, newScore, accepted)
    else
      (variables, score, accepted)
  }
}

private object WalkersChain {
  def apply(density: DensityFunction, nWalkers: Int)(
      implicit rng: RNG): WalkersChain = {
    val walkers = 1
      .to(nWalkers)
      .map { _ =>
        1.to(density.nVars)
          .map { _ =>
            rng.standardNormal
          }
          .toArray
      }
      .toVector
    val scores = walkers.map { array =>
      density.update(array)
      density.density
    }
    WalkersChain(density, walkers, scores, true, 0)
  }
}
