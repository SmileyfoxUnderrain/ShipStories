package logic

import ships.ShipBase

class Navy(vararg shipsOrder: Pair<Int,Int>){
    private val order = shipsOrder
    val ships: List<ShipBase>
        get() = order.map{p ->
                IntProgression.fromClosedRange(0, p.first-1, 1)
                .map{ShipBase(p.second)}}
            .flatten()

}

