package logic.HitCalculators

import area.Coords
import area.EnemyArea
import logic.Navy
import ships.Deck
import ships.ShipBase
import kotlin.math.*


open class SmartCalculator : HitCalculator {
    private val undestroyedShips = mutableListOf<ShipBase>()

    override fun calculateHit(enemyArea: EnemyArea, navy: Navy): Coords {
        undestroyedShips.clear()
        undestroyedShips.addAll(detectUndestroyedShips(enemyArea, navy))
        // Если можно рассчитать потенциальные координаты
        if(enemyArea.hitDecks.any() && undestroyedShips.any{it.size > 1}) {
            val hhShips = detectHalfHitShips(enemyArea, navy)
            val shipToHit = hhShips
                // Этот и последующий фильтры не должны срабатывать, если нет ошибок в других местах.
                // Если что-то подпадает под этот или следующий фильтр, необходимо уточнять, все ли данные по
                // потопленным кораблям были переданы
                .filter { it.potentialCoords.any() }
                .minByOrNull { it.potentialCoords.size }

            if(shipToHit != null){
                val knownDecksCoords = shipToHit.knownDecks.map { kd -> kd.coords }
                // Возвращать самую близкую к координатам известных Deck координату
                return shipToHit.potentialCoords
                    .sortedBy { c ->
                        knownDecksCoords.sumBy { kdc ->
                            abs(c.x - kdc.x) + abs(c.y - kdc.y)
                        }
                    }
                    .first()
            }
        }
        // Если рассчитать потенциальные координаты нельзя, или что-то пошло не так, пытаемся оценить
        // вероятность наличия корабля в каждой свободной ячейке одним из алгоритмов:
        // 1. Путём перебора ячеек
        // 2. Путём создания и оценки копий enemyArea c дорасположением туда нетронутых кораблей
        // с последующим наложением этих копий и вычислением вероятности
        val cells = calculatePotentialCells(enemyArea)
        val retcoords = cells.groupBy { c ->
            var ret: Double
            if(c.coords.x == 0 || c.coords.y == 0 || c.coords.x == 9 || c.coords.y == 9){
                // Коэффициент влияния расположения корабля скраю карты:
                // такое расположение увеличивает шанс выигрыша из-за большего количества
                // пустых клеток
                ret = (c.potentialShips.size) * 1.2
            }
            else
                ret = c.potentialShips.size.toDouble()
            ret
            }
            .maxByOrNull { it.key }?.value?.random()?.coords!!
        return retcoords
    }
    private fun detectUndestroyedShips(enemyArea: EnemyArea, navy: Navy): MutableList<ShipBase>{
        val undestroyedShips = navy.ships.toMutableList()
        for(placedShip in enemyArea.placedShips){
            if(navy.ships.any{it.size == placedShip.size})
                undestroyedShips.remove(undestroyedShips.first { it.size == placedShip.size })
        }
        return undestroyedShips
    }

    private fun detectHalfHitShips(enemyArea: EnemyArea, navy: Navy): List<HalfHitShip>{
        val halfHitShips = mutableListOf<HalfHitShip>()
        val sortedHitDecks = enemyArea.hitDecks.distinct().sortedBy { it.coords.y }.sortedBy { it.coords.x }
        for(deck in sortedHitDecks) {
            if(halfHitShips.flatMap { it.knownDecks }.contains(deck))
                continue

            val halfHitShip = HalfHitShip(
                undestroyedShips.filter { it.size > 1 }.toMutableList(),
                mutableListOf<Deck>(),
                mutableSetOf<Coords>())
            halfHitShips.add(halfHitShip)
            defineUndestroyedShipRecursive(deck, enemyArea, halfHitShips, halfHitShip )
            calculatePotentialShipCoords(halfHitShip, enemyArea)
        }

        // На данном этапе количество потенциально подходящих кораблей для групп координат
        // будет больше, чем их полный список
        return halfHitShips
    }

    private fun defineUndestroyedShipRecursive(
        hitDeck: Deck,
        enemyArea: EnemyArea,
        halfHitShips: List<HalfHitShip>,
        halfHitShip: HalfHitShip){
        // Передаём Deck, которая будет относится к нашему HalfHitShip и ищем для неё
        // другие соседствующие Deck-и, за исключением тех, что уже входят в состав HalfHitShip
        halfHitShip.knownDecks.add(hitDeck)

        val neighboringDecks = enemyArea.findNeighboringHitDecks(hitDeck)
            .minus(halfHitShips.flatMap { it.knownDecks })

        halfHitShip.potentialShips.removeIf { it.size < halfHitShip.knownDecks.size }

        for(deck in neighboringDecks)
            defineUndestroyedShipRecursive(
                deck, enemyArea, halfHitShips, halfHitShip)
    }

    private fun calculatePotentialShipCoords(ship: HalfHitShip, enemyArea: EnemyArea){
        val maxPotentialSize = ship.potentialShips.map{ it.size }.maxOrNull() ?: return
        val notFreeCells = enemyArea.placedShips.flatMap { it.safetyCoords }.plus(enemyArea.missedHitsCoords)
        val maxdelta = maxPotentialSize - ship.knownDecks.size
        val coords = mutableSetOf<Coords>()
        if(ship.knownDecks.size > 1){
            for(deck in ship.knownDecks) {
                val coordsForDek = allCoordsInDeltas(deck.coords, maxdelta, ship.isHorizontal)
                coords.addAll(coordsForDek)
            }
        }
        else{
            val knownDeckCoords = ship.knownDecks.first().coords
            val horizontalCoords = allCoordsInDeltas(knownDeckCoords, maxdelta, true)
            coords.addAll(horizontalCoords)
            val verticalCoords = allCoordsInDeltas(knownDeckCoords, maxdelta, false)
            coords.addAll(verticalCoords)
        }
        // Удалить из списка те координаты, которые приходятся:
        // 1) На другие Decks, принадлежащие только placedShips и на известные SafetyArea, принадлежащие plaсedShips
        // 2) На другие HitDecks
        // 3) На известные missedHitsCoords
        coords.removeIf { enemyArea.occupiedCells.contains(it) }

        if(coords.size < maxPotentialSize)
            ship.potentialShips.removeIf { it.size > coords.size }

        // Дополнительно отфильтровываем из списка потенциалоьных координат те, что принадлежат KnownDecks
        coords.removeIf{ c -> ship.knownDecks.map{it.coords}.contains(c) }
        ship.potentialCoords.addAll(coords)
    }

    private fun allCoordsInDeltas(coords: Coords, delta: Int, isHorizontal: Boolean): List<Coords>{
        // constant coord value
        val c = if(isHorizontal) coords.y else coords.x
        // variable coord value
        val v = if(isHorizontal) coords.x else coords.y

        val onStart = IntProgression.fromClosedRange(v, v + delta, 1)
        val onEnd = IntProgression.fromClosedRange(v - delta, v, 1)
        val allrange = onStart.plus(onEnd).distinct().filter { it in 0..9 }
        val retCoords = allrange.map{
            if(isHorizontal)
                coords.copy(x = it, y = c)
            else
                coords.copy(x = c, y = it)
        }
        return retCoords
    }

//    private fun filterUnreachableCells(
//        coords: Coords, potentialCoords: MutableSet<Coords>, notFreeCells: List<Coords>){
//        if(potentialCoords == null)
//            return
//        val isHorizontal = coords.y == potentialCoords.firstOrNull()?.y
//        if(isHorizontal){
//            val sortedCoords = potentialCoords.sortedBy { it.x }
//            val begin = sortedCoords.first().x
//            val middle = coords.x
//            val end = sortedCoords.last().x
//            val beginToMiddleNotFreeCells = notFreeCells.filter{ it.x in begin until middle }
//
//        }
//    }
    private fun calculatePotentialCells(enemyArea: EnemyArea):List<PotentialHitCell>{
        val ret = mutableListOf<PotentialHitCell>()
        for(cellCoords in enemyArea.untouchedCells){
            val cell = PotentialHitCell(cellCoords, mutableListOf<ShipBase>())
            ret.add(cell)
            setPotentialShipsForCell(enemyArea, cell)
        }
        return ret
    }

    private fun setPotentialShipsForCell(enemyArea: EnemyArea, cell: PotentialHitCell ){
        // Ячейке автоматически соответствуют все оставшиеся непотопленные однопалубные корабли
        cell.potentialShips.addAll(undestroyedShips.filter { it.size == 1 })
        val longships = undestroyedShips.filter{it.size != 1}
        if(!longships.any())
            return

        for(ship in longships) {
            val horizontals = allCoordsInDeltas(cell.coords, ship.size - 1, true)
            handleCoordsInDeltas(cell, ship, horizontals.toMutableSet(), enemyArea)
            val verticals = allCoordsInDeltas(cell.coords, ship.size - 1, false)
            handleCoordsInDeltas(cell, ship, verticals.toMutableSet(), enemyArea)
        }
    }

    private fun handleCoordsInDeltas(
        cell: PotentialHitCell, ship: ShipBase, potentialCoords: MutableSet<Coords>, enemyArea: EnemyArea){
        potentialCoords.removeIf { !enemyArea.untouchedCells.contains(it) }
        if(potentialCoords.size >= ship.size)
            for(i in ship.size .. potentialCoords.size)
                cell.potentialShips.add(ship)
    }

}
class HalfHitShip(val potentialShips: MutableList<ShipBase>,
                  val knownDecks: MutableList<Deck>,
                  val potentialCoords: MutableSet<Coords>
                  ){
    val isHorizontal: Boolean
        get() = knownDecks.size > 1
            && knownDecks.map{it.coords}.first().y == knownDecks.map{it.coords}.last().y
}

class PotentialHitCell(val coords: Coords, val potentialShips: MutableList<ShipBase>)

