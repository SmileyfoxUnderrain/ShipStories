package logic.HitCalculators

import area.Coords
import area.EnemyArea
import logic.Navy

class DumpCalculator: HitCalculator{
    override fun calculateHit(enemyArea: EnemyArea, navy: Navy): Coords {
        return enemyArea.untouchedCells.random()
    }
}