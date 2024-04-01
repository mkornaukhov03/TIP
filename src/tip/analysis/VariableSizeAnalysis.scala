package tip.analysis

import tip.ast.{AAssignStmt, ADeclaration, AIdentifier, AVarStmt, NoCalls, NoPointers, NoRecords}
import tip.cfg._
import tip.ast.AstNodeData.DeclarationData
import tip.lattices.IntervalLattice._
import tip.lattices._
import tip.solvers._

trait VariableSizeAnalysis extends ValueAnalysisMisc with Dependencies[CfgNode] {

  import tip.cfg.CfgOps._

  val cfg: ProgramCfg

  val valuelattice: IntervalLattice.type

  val liftedstatelattice: LiftLattice[statelattice.type]

  /**
    * Int values occurring in the program, plus -infinity and +infinity.
    */
  private val B = cfg.nodes.flatMap { n =>
    n.appearingConstants.map { x =>
      IntNum(x.value): Num
    } + MInf + PInf
  }

  private val WIntervals = List[IntervalLattice.Element](
    (0: Num, 1: Num), // bool
    (-(2 << 7): Num, (2 << 7) - 1: Num), // byte
    (0, (1 << 16) - 1), // char
    (Int.MinValue: Num, Int.MaxValue: Num), // int
    (MInf, PInf) // unbounded
  )

  def find(x: valuelattice.Element): valuelattice.Element = {
    val left = x._1
    val right = x._2
    for (p <- WIntervals) {
      if (p._1 <= left && right <= p._2) {
        return p
      }
    }
    (MInf, PInf)
  }

  def loophead(n: CfgNode): Boolean = indep(n).exists(cfg.rank(_) > cfg.rank(n))

  def widenInterval(x: valuelattice.Element, y: valuelattice.Element): valuelattice.Element =
    (x, y) match {
      case (IntervalLattice.EmptyInterval, _) => find(y)
      case (_, IntervalLattice.EmptyInterval) => find(x)
      case ((l1, h1), (l2, h2)) => find(IntervalLattice.min(Set(l1, l2)), IntervalLattice.max(Set(h1, h2)))
    }

  def widen(x: liftedstatelattice.Element, y: liftedstatelattice.Element): liftedstatelattice.Element =
    (x, y) match {
      case (liftedstatelattice.Bottom, _) => y
      case (_, liftedstatelattice.Bottom) => x
      case (liftedstatelattice.Lift(xm), liftedstatelattice.Lift(ym)) =>
        liftedstatelattice.Lift(declaredVars.map { v =>
          v -> widenInterval(xm(v), ym(v))
        }.toMap)
    }
}

object VariableSizeAnalysis {

  object Intraprocedural {

    /**
      * VariableSize analysis, using the worklist solver with init and widening.
      */
    class WorklistSolverWith(cfg: IntraproceduralProgramCfg)(implicit declData: DeclarationData)
        extends IntraprocValueAnalysisWorklistSolverWithReachability(cfg, IntervalLattice)
        with WorklistFixpointSolverWithReachabilityAndWidening[CfgNode]
        with VariableSizeAnalysis

    /**
      * VariableSize analysis, using the worklist solver with init, widening, and narrowing.
      */
    class WorklistSolverWithAndNarrowing(cfg: IntraproceduralProgramCfg)(implicit declData: DeclarationData)
        extends IntraprocValueAnalysisWorklistSolverWithReachability(cfg, IntervalLattice)
        with WorklistFixpointSolverWithReachabilityAndWideningAndNarrowing[CfgNode]
        with VariableSizeAnalysis {

      val narrowingSteps = 5
    }
  }
}
