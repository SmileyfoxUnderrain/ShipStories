
import area.Area
import area.EnemyArea
import logic.HitCalculators.DumpCalculator
import logic.HitCalculators.SmartCalculator
import logic.Navy
import logic.Player

fun main(args: Array<String>) {
    val navy = Navy(Pair(4,1), Pair(3,2), Pair(2,3), Pair(1,4))
    val area = Area(10, 10)
    val enemyArea = EnemyArea(10,10)

    val player1 = prepareFirstPlayer()
    val player2 = Player("Player 2", navy, area, enemyArea, SmartCalculator())
    player2.autoPlaceShips()
    player2.area.printCellsInPseudographic()

    var leader = player1
    var opponent = player2
    while(player1.unhitDecks > 0 && player2.unhitDecks > 0) {
        if(!hitOneAnother(leader, opponent)) {
            val t = leader
            leader = opponent
            opponent = t
        }

        println("Player1: ${player1.unhitDecks}, Player2: ${player2.unhitDecks} ")
        player1.area.printCellsInPseudographic()
        println()
        //player1.enemyArea.printCellsInPseudographic()
        player2.area.printCellsInPseudographic()
        println()
        player2.enemyArea.printCellsInPseudographic()
        //readLine()
    }
}

fun prepareFirstPlayer(): Player{
    val navy = Navy(Pair(4,1), Pair(3,2), Pair(2,3), Pair(1,4))
    val area = Area(10, 10)
    val enemyArea = EnemyArea(10,10)
    val calculator = SmartCalculator()
    val player = Player("Player 1", navy, area, enemyArea, calculator)
    player.autoPlaceShips()
    player.area.printCellsInPseudographic()
    return player
}

fun hitOneAnother(playerA: Player, playerB: Player): Boolean{
    val hitRequest = playerA.getHitRequest()
    val hitReply = playerB.hit(hitRequest)
    playerA.parseHitReply(hitRequest.coords, hitReply)
    return hitReply.isImpact
}
