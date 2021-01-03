package logic

import area.Area
import area.Coords
import area.EnemyArea
import logic.hitCalculators.HitCalculator
import ships.Ship
import ships.Deck

open class Player(
    val name: String, val navy: Navy, val area: Area, val enemyArea: EnemyArea, val calculator: HitCalculator){

    var placementsCount: Int = 0

    val unhitDecks: Int
        get() = area.placedShips.flatMap { it.decksCoorded }.count {!it.isHit}

    fun autoPlaceShips(){
        val ships = navy.ships.toMutableList()
        while(ships.any()){
            val ship = ships.random()
            val places = area.placesForShip
                    .filter { it.xLength >= ship.size || it.yLength >= ship.size }
            if(!places.any()) {
                area.clear()
                autoPlaceShips()
                return
            }
            else {
                val place = places.random()
                val shipToPlace = Ship(ship, place.yLength <= place.xLength, place.topLeftCoords)
                area.placeShip(shipToPlace)
                ships.remove(ship)
                placementsCount++
            }
        }
    }
    fun parseHitReply(coords: Coords, hitReply: HitReply){
        println("$name: $coords")
        println("$hitReply")
        if(!hitReply.isImpact) {
            hitMissed(coords)
            return
        }

        hitSuccessful(coords)
        if(hitReply.destroyedShip != null)
            enemyArea.addDefeatedShip(hitReply.destroyedShip)
    }

    fun hit(hitrequest: HitRequest): HitReply{
        val hitDeck = area.getDeckOnCoordsOrNull(hitrequest.coords)
        if(hitDeck == null) {
            area.missedHitsCoords.add(hitrequest.coords)
            return HitReply(false, null)
        }

        hitDeck.isHit = true;
        val destroyedShip = area.getDestroyedShipOrNull(hitrequest.coords) ?:
            return HitReply(true, null)

        return HitReply(true, destroyedShip)
    }

    fun getHitRequest(): HitRequest = HitRequest(calculateCoordstoHitEnemy())

    fun calculateCoordstoHitEnemy(): Coords{
        //val calculator: HitCalculator = DumpCalculator()
        return calculator.calculateHit(enemyArea, navy)
    }

    fun hitSuccessful(coords: Coords){
        enemyArea.addHitDeck(Deck(0, true, coords))
    }

    fun hitMissed(coords: Coords){
        enemyArea.missedHitsCoords.add(coords)
    }
}