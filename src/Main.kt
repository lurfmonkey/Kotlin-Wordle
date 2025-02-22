import java.util.*
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader

var guessedLetters = mutableMapOf<Char, Int>()
var prevGuesses = mutableListOf<String>()

// The letters in each row of a QWERTY keyboard
val qwertyKeyboard = "QWERTYUIOP\nASDFGHJKL\nZXCVBNM"

// Letter background colors
val redBkg = "\u001B[41m"
val greenBkg = "\u001B[42m"
val yellowBkg = "\u001B[43m"
val colorEnd = "\u001B[0m"

fun main() {
    while (true) {
        // Choose a random word from a fixed list
        val correctWord = getRandomWord()

        // Play the game until the user no longer wants to play
        if (!playGame(correctWord))
            break
    }
}

// Plays a game using the given word
fun playGame(word: String) : Boolean {
    guessedLetters.clear()
    prevGuesses.clear()

    // Give the user instructions
    printInstructions()

    var guessNumber = 1

    while (true) {
        // Ask the user for a guess
        print("Guess number $guessNumber: ")
        var curGuess = readLine()

        // Check if the guess is valid. Otherwise ask for the guess again
        if (!isValidGuess(curGuess)) {
            println("Guess must be a 5 letter word")
            continue
        }

        if (curGuess != null) {
            curGuess = curGuess.uppercase(Locale.getDefault())
            guessNumber++

            // Check the guess to see if letters are in the word and in the right place
            val guessResults : List<Int> = checkGuess(curGuess, word)
            var analyzedGuess = ""

            // Format the guess to indicate which letters are in the word and/or correct
            for (i in 0..4) {
                when (guessResults[i]) {
                    0 -> {
                        // The letter is not in the word
                        analyzedGuess += " ${curGuess[i]} "
                    }
                    1 -> {
                        // The letter is in the word but in the wrong place
                        // Indicate using yellow
                        analyzedGuess += "$yellowBkg ${curGuess[i]} $colorEnd"
                    }
                    2 -> {
                        // The letter is in the word and in the correct place
                        // Indicate using green
                        analyzedGuess += "$greenBkg ${curGuess[i]} $colorEnd"
                    }
                }
            }

            // Print the previous guesses and the formatted guess
            println()
            var guessNum = 1
            for (prevGuess in prevGuesses) {
                println("$guessNum: $prevGuess")
                guessNum++
            }

            println("$guessNum: $analyzedGuess")
            // Add the formatted guess to the previous guesses
            prevGuesses.add(analyzedGuess)

            // The word has been guessed correctly so end the game
            if (guessResults == listOf(2, 2, 2, 2, 2)) {
                println("--------")
                println("Correct!")
                println("--------")
                break
            }

            //The user did not guess the word in 6 tries so end the game
            if (guessNumber > 6) {
                println("--------")
                println("Sorry. The word was $word")
                println("--------")
                break
            }

            // Show the keyboard with the status of the guessed letters
            showKeyboard()
        }
    }

    // Ask the user if they want to play again.
    print("Play again? Y/N ")
    if (readln().uppercase() == "Y" )
        return true
    return false
}

// Checks the guess to see which letters are in the word and in the right place
fun checkGuess(guess: String, word: String) : MutableList<Int> {
    // List to store the state of each letter guessed
    // 0 - Letter not in word
    // 1 - Letter in word but in wrong place
    // 2 - Letter in word in correct place
    val guessResultsList: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0)
    val correctLettersMap: MutableMap<Char, Int> = mutableMapOf()

    for (i in 0..4) {
        //The letter is in the correct place
        if (guess[i] == word[i]) {
            //Keep track of number of correct occurrences
            if (!correctLettersMap.containsKey(guess[i])) {
                correctLettersMap[guess[i]] = 1
            } else {
                val flag = correctLettersMap[guess[i]]

                if (flag != null) {
                    correctLettersMap[guess[i]] = flag + 1
                }
            }
            // Flag letter as correctly guessed
            guessResultsList[i] = 2

            // Add letter to list of correct guesses in the right place
            guessedLetters[guess[i]] = 2
        }
    }
    // Look for letters in the word but not in the right place
    for (i in 0..4) {
        for (j in 0..4) {
            // The letter has been found in the word
            if (guess[i] == word[j]) {
                // If not in the correct place, keep track of the number of occurrences
                if (j != i) {
                    if (!correctLettersMap.containsKey(guess[i])) {
                        correctLettersMap[guess[i]] = 1
                    } else {
                        val flag = correctLettersMap[guess[i]]

                        if (flag != null) {
                            correctLettersMap[guess[i]] = flag + 1
                        }
                    }

                    // Get the number of times the letter is in the word
                    val timesInWord = word.count { it == guess[i] }

                    // If the number of occurrences so far is less than or equal to the number
                    // of times in the word, flag the letter as in the word unless it has already
                    // been found to be in the correct place
                    if (correctLettersMap[guess[i]]!! <= timesInWord) {
                        if (guessResultsList[i] != 2) {
                            guessResultsList[i] = 1
                        }
                    }
                    break
                }
            }
        }
    }

    // Update the map of letters guessed that are either in or not in the word
    for (i in 0..4) {
        when (guessResultsList[i]) {
            0 -> {
                if (!guessedLetters.containsKey(guess[i])) {
                    guessedLetters[guess[i]] = 0
                }
            }

            1 -> {
                if (guessedLetters.containsKey(guess[i])) {
                    // Flag the letter as in the word if it has not already been flagged as in the correct place
                    if (guessedLetters[guess[i]] != 2) {
                        guessedLetters[guess[i]] = 1
                    }
                } else {
                    guessedLetters[guess[i]] = 1
                }
            }
        }
    }

    return guessResultsList
}

// Checks to see if the word in alpha, 5 letters long, and in the English dictionary
fun isValidGuess(guess: String?) : Boolean {
    if (guess != null && guess.length == 5 && guess.all {it.isLetter()}) {
        // Check to see if the word is in the English dictionary using Wordnik
        val url = "https://api.wordnik.com/v4/word.json/${guess.lowercase()}/definitions?limit=200&includeRelated=false&useCanonical=false&includeTags=false&api_key=eqjbxg50ocrsuuzsrbr7iihx3dacv9av08yq9jvbyj122546g"
        val jsonBody = """{"key": "value"}"""
        val response = makeJsonRequest(url, "GET", jsonBody)
        if (response != null) {
            if (response.contains("\"word\":\"$guess\"")) {
                return true
            }
            else {
                println("${guess.uppercase()} not in English dictionary")
            }
        }
    }

    return false
}

// Shows the keyboard with the state of each guessed letter
fun showKeyboard() {
    println("------------------------------")
    println(getFormattedAlphabet(qwertyKeyboard, guessedLetters))
    println("------------------------------")
}

// Formats the letters in the keyboard based on the state of each guessed letter
// Green for letter guessed correctly in the right place
// Yellow for letter guessed correctly but in the wrong place
// Red for letter not in word
// No background color for letters that have not yet been guessed
fun getFormattedAlphabet(keyboard: String, guessed: MutableMap<Char, Int>): String {
    var formattedRow = ""
    for (letter in keyboard) {
        if (letter in guessed) {
            when(guessed[letter]) {
                0 -> {
                    // The letter is not in the word
                    formattedRow += "$redBkg $letter $colorEnd"
                }
                1 -> {
                    // The letter is in the word but has not been guessed in the correct place
                    formattedRow += "$yellowBkg $letter $colorEnd"
                }
                2 -> {
                    // The letter has been guessed in the correct place
                    formattedRow += "$greenBkg $letter $colorEnd"
                }
            }
        }
        else {
            formattedRow +=  if (letter != '\n') " $letter " else letter
        }
    }

    return formattedRow
}

// Makes a JSON request to the given URL and returns the response in string form
fun makeJsonRequest(url: String, method: String, jsonBody: String? = null): String? {
    return try {
        val urlObj = URI(url).toURL()
        val connection = urlObj.openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doInput = true

        if (method == "POST" || method == "PUT") {
            connection.doOutput = true
            val outputStreamWriter = OutputStreamWriter(connection.outputStream)
            if (jsonBody != null) {
                outputStreamWriter.write(jsonBody)
            }
            outputStreamWriter.flush()
            outputStreamWriter.close()
        }
        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                response.append(line)
            }
            bufferedReader.close()
            connection.disconnect()
            response.toString()
        } else {
            connection.disconnect()
            "Error: $responseCode"
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

// Print a list of instructions
fun printInstructions() {
    println("--------------------------------")
    println("|        KOTLIN WORDLE         |")
    println("--------------------------------")
    println("Guess a 5 letter word")
    println("$yellowBkg   $colorEnd indicates the letter is in the word but not in the correct place")
    println("$greenBkg   $colorEnd indicates the letter is in the word in the correct place")
    println("$redBkg   $colorEnd indicates the letter is not in the word")

    showKeyboard()
}

// Returns a random word from a fixed set
fun getRandomWord(): String {
    val possibleWords = setOf(
        "CRAZY",
        "THIEF",
        "DOILY",
        "FEAST",
        "CIVIC",
        "USURP",
        "APTLY",
        "GHOST",
        "QUARK",
        "PINCH",
        "CHUCK",
        "PLUMP",
        "ASSET",
        "CELLO",
        "MOMMY",
        "DENSE",
        "COUNT",
        "FIRST",
        "THIRD",
        "CHIEF",
        "HEARD",
        "DROWN",
        "FREAK",
        "VALVE",
        "BRINK",
        "HOARD",
        "ANGLE",
        "CLOUD",
        "UNDER",
        "WRIST",
        "BLURB",
        "CRAWL",
        "AISLE",
        "POUND",
        "FRANK",
        "EXACT",
        "MIMIC",
        "AVOID",
        "PLAID",
        "GROIN",
        "CACAO",
        "LANAI",
        "CANAL",
        "CRASH",
        "GROUP",
        "HIPPY",
        "BEING",
        "SHOOT",
        "PLAZA",
        "GHOUL"
    )

    return possibleWords.random()
}