package logic

import area.Coords
import ships.Ship

data class HitRequest(val coords: Coords)
data class HitReply(val isImpact: Boolean, val destroyedShip: Ship?)
