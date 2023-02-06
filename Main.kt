package minesweeper

import kotlin.random.Random

enum class Messages (val str: String) {
    MineCount("How many mines do you want on the field?"),
    Prompt("Set/unset mines marks or claim a cell as free:"),
    NumberError("There is a number here!"),
    Victory("Congratulations! You found all the mines!"),
    Failure("You stepped on a mine and failed!")
}

class MineSweeper(private val fieldX: Int, private val fieldY: Int) {
    private val field: MutableList<MutableList<String>> = mutableListOf()
    private var playerField: MutableList<MutableList<String>> = mutableListOf()
    private var mines = 0
    private var minesMarked = 0
    private val minesCoordinates = mutableListOf<Pair<Int, Int>>()
    private val offsets = listOf(
        Pair(1, 1), Pair(1, 0), Pair(1, -1), Pair(-1, 1),
        Pair(-1, 0),Pair(-1, -1), Pair(0, 1), Pair(0, -1)
    )
    private val freeChainListToDo = mutableListOf<Pair<Int, Int>>()
    private val freeChainListDone = mutableListOf<Pair<Int, Int>>()
    private var isGameOngoing = true

    init {
        println(Messages.MineCount.str)
        mines = readln().toInt()

        fieldSetup()
        printPlayerField()

        while (isGameOngoing) {
            printPlayerField()
            println(Messages.Prompt.str)
            val (moveX, moveY, action) = readln().split(' ')

            when(action) {
                "free" -> free(moveX.toInt() - 1, moveY.toInt() - 1)
                "mine" -> mark(moveX.toInt() - 1, moveY.toInt() - 1)
            }
        }
    }

    private fun freeChain(moveX: Int, moveY: Int) {
        freeChainListDone.add(Pair(moveX, moveY))

        for (pair in offsets) {
            if(
                field.getOrNull(moveY + pair.first)?.
                getOrNull(moveX + pair.second) == SLASH &&
                Pair(moveX + pair.second, moveY + pair.first) !in freeChainListDone
            ) {
                freeChainListToDo.add(Pair(moveX + pair.second, moveY + pair.first))
                playerField[moveY + pair.first][moveX + pair.second] = SLASH
            }
        }

        freeChainListToDo.toMutableList().forEach { pair ->
            if(Pair(pair.first, pair.second) !in freeChainListDone.toMutableList()) {
                freeChain(pair.first, pair.second)
            }
        }

        freeChainListDone.forEach { pair ->
            offsets.forEach { _pair ->
                if(isNumber(field.getOrNull(pair.second + _pair.first)?.
                    getOrNull(pair.first + _pair.second))) {
                    playerField[pair.second + _pair.first][pair.first + _pair.second] =
                        field[pair.second + _pair.first][pair.first + _pair.second]
                }
            }
        }

        var count = 0
        playerField[0].forEach { if (it == SLASH || it.matches(Regex("^[1-9]$"))) count++ }
        victory(count, "fully explored")
    }

    private fun free(moveX: Int, moveY: Int) {
        if (playerField[moveY][moveX] == STAR ||
            playerField[moveY][moveX].contains(Regex("^[1-9]$")) ||
            playerField[moveY][moveX] == SLASH) {
            return
        } else if (field[moveY][moveX] == X) {
            victory(type = "stepped on it")
        } else if (field[moveY][moveX].contains(Regex("^[1-9]$"))) {
            playerField[moveY][moveX] = field[moveY][moveX]
        } else {
            playerField[moveY][moveX] = field[moveY][moveX]
            freeChain(moveX, moveY)

        }
        var count = 0
        for (list in 0..playerField.lastIndex) {
            for (str in 0..playerField[list].lastIndex) {
                if (playerField[list][str] == SLASH || playerField[list][str].
                    matches(Regex("^[1-9]$"))) count++
            }
        }
        victory(count, "fully explored")
    }

    private fun mark(moveX: Int, moveY: Int) {
        if (playerField[moveY][moveX] == STAR && field[moveY][moveX] == X) {
            playerField[moveY][moveX] = DOT
            minesMarked--
        } else if (field[moveY][moveX] == X && playerField[moveY][moveX] == DOT) {
            minesMarked++
            playerField[moveY][moveX] = STAR
        } else if (playerField[moveY][moveX] == STAR) {
            playerField[moveY][moveX] = DOT
        } else if (playerField[moveY][moveX].contains(Regex("^[1-9]$")) ||
            playerField[moveY][moveX] == SLASH) {
            println(Messages.NumberError.str)
            return
        } else if (playerField[moveY][moveX] == DOT) playerField[moveY][moveX] = STAR
        victory(minesMarked, "mines found")
    }

    private fun fieldSetup() {
        for (y in 0 until fieldY) {
            field.add(mutableListOf())
            repeat(fieldX) { field[y] += DOT }
        }
        playerField = field.map { it.toMutableList() }.toMutableList()

        minesRandomSet()

        for (y in 0 until fieldY) {
            for (x in 0 until fieldX) {
                var nearMines = 0
                if (field[y][x] == DOT) {
                    for (pair in offsets) {
                        if(isX(field.getOrNull(y + pair.first)?.
                            getOrNull(x + pair.second))) nearMines++
                    }
                    if (nearMines != 0) {
                        field[y][x] = nearMines.toString()
                        continue
                    }
                    field[y][x] = SLASH
                }
            }
        }
    }

    private fun minesRandomSet() {
        var minesToSet = if (mines > (fieldX * fieldY) / 2 ) fieldX * fieldY - mines else mines
        if (minesToSet != mines) {
            for (y in 0 until fieldY) {
                for (x in 0 until fieldX) {
                    field[y][x] = X
                    minesCoordinates.add(Pair(x, y))
                }
            }
            while (minesToSet != 0) {
                val randomX = Random.nextInt(0, fieldX)
                val randomY = Random.nextInt(0, fieldY)
                if (Pair(randomX, randomY) !in minesCoordinates) continue
                minesCoordinates.remove(Pair(randomX, randomY))
                field[randomY][randomX] = DOT
                minesToSet--
            }
        } else {
            while (minesToSet != 0) {
                val randomX = Random.nextInt(0, fieldX)
                val randomY = Random.nextInt(0, fieldY)
                if (Pair(randomX, randomY) in minesCoordinates) continue
                field[randomY][randomX] = X
                minesCoordinates.add(Pair(randomX, randomY))
                minesToSet--
            }
        }
    }

    private fun printPlayerField() {
        print(SPACE + VERTICAL_BAR).also {
            repeat(fieldY) { print(it + 1) }; println(VERTICAL_BAR)
        }
        print(BAR + VERTICAL_BAR).also {
            repeat(fieldY) { print(BAR) }; println(VERTICAL_BAR)
        }
        playerField.forEachIndexed { index, list ->
            print("${index + 1}$VERTICAL_BAR")
            list.forEach { str -> print(str) }
            print(VERTICAL_BAR)
            println()
        }
        print(BAR + VERTICAL_BAR).also {
            repeat(fieldY) { print(BAR) }; println(VERTICAL_BAR)
        }
    }

    private fun victory(n: Int = 0, type: String) {
        if (n == fieldX * fieldY - mines && type == "fully explored") {
            printPlayerField()
            println(Messages.Victory.str)
            isGameOngoing = false
        } else if (type == "stepped on it") {
            for (pair in minesCoordinates) playerField[pair.first][pair.second] = X
            printPlayerField()
            println(Messages.Failure.str)
            isGameOngoing = false
        } else if (n == mines && type == "all mines found") {
                printPlayerField()
            println(Messages.Victory.str)
                isGameOngoing = false
        }
    }

    private fun isX(str: String?): Boolean = str == X

    private fun isNumber(str: String?): Boolean = (str != null && str.
    contains(Regex("^[1-9]$")))

    companion object {
        const val DOT = "."
        const val X = "X"
        const val VERTICAL_BAR = "│"
        const val BAR = "—"
        const val SPACE = " "
        const val STAR = "*"
        const val SLASH = "/"
    }
}

fun main() {
    MineSweeper(9, 9)
}