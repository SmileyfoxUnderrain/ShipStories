package logic.hitCalculators

import area.Coords
import area.EnemyArea
import logic.Navy

interface HitCalculator {
    fun calculateHit(enemyArea: EnemyArea, navy: Navy):Coords
}