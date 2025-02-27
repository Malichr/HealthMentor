package com.example.healthmentor

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

object AIAdviceService {
    private const val API_KEY = "AIzaSyDqwyjQDbOMACZoufG6sgy8eBRD7DpDarc"

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-8b",
        apiKey = API_KEY
    )

    private var chat: Chat? = null

    suspend fun getAIAdvice(fitnessData: FitnessDataState): String {
        return withContext(Dispatchers.IO) {
            try {
                if (chat == null) {
                    chat = model.startChat()
                }
                
                val hungarianPrompt = """
                    Feladat: Elemezd a felhasználó Google Fit adatait, és adj személyre szabott egészségügyi vagy aktivitási tanácsokat. 
                    A tanácsok legyenek praktikusak, motiválóak, és figyelembe vegyék a felhasználó eddigi aktivitását.

                    Adatok:
                    - Napi lépésszám: ${fitnessData.dailySteps} lépés
                    - Napi távolság: ${String.format("%.1f", fitnessData.dailyDistance)} méter
                    - Átlagsebesség: ${String.format("%.1f", fitnessData.speed * 3.6)} km/h
                    - Napi kalória: ${fitnessData.dailyCalories} kcal
                    - Aktív kalóriák: ${fitnessData.dailyActiveCalories} kcal
                    
                    Statisztikák:
                    - Átlagos napi lépésszám: ${fitnessData.avgStepsPerDay} lépés
                    - Átlagos napi kalória: ${fitnessData.avgCaloriesPerDay} kcal
                    - Aktív napok száma: ${fitnessData.activeDays} nap
                    - Jelenlegi sorozat: ${fitnessData.currentStreak} nap
                    - Leghosszabb sorozat: ${fitnessData.longestStreak} nap

                    Kérlek, használj Markdown formázást a válaszodban:
                    - Használj **félkövér** szöveget a fontos részek kiemelésére
                    - A főbb szekciókat # címsorral jelöld
                    - A részleteket ## alcímekkel tagold
                    - Használj felsorolásjeleket (-)

                    Elvárt formátum:
                    # Aktivitási értékelés
                    **Aktivitási szinted: [alacsony/közepes/magas]**

                    ## Fő tanács
                    - [Személyre szabott tanács az adatok alapján]

                    ## Napi tipp
                    - [Konkrét, megvalósítható javaslat]

                    ## Motiváció
                    **[Motivációs üzenet]**

                    Stílus: Pozitív, barátságos, motiváló, és érthető magyar nyelven.
                """.trimIndent()

                try {
                    val response = chat?.sendMessage(hungarianPrompt)
                    response?.text ?: "Sajnos nem sikerült tanácsot generálni. Kérlek próbáld újra később."
                } catch (e: Exception) {
                    Log.e("AIAdviceService", "Hiba a chat üzenet küldésekor: ${e.message}", e)
                    "Átmeneti hiba történt a tanácsadás során. Kérlek próbáld újra később."
                }
            } catch (e: Exception) {
                Log.e("AIAdviceService", "Hiba a Gemini API hívásakor: ${e.message}", e)
                """
                Jelenleg nem érhető el az AI tanácsadás. 
                
                Általános tanácsok az egészséges életmódhoz:
                1. Mozogj rendszeresen, legalább napi 30 percet
                2. Figyelj az egészséges táplálkozásra
                3. Aludj eleget, napi 7-8 órát
                4. Igyál elegendő vizet
                
                Ne add fel, minden kis lépés számít az egészséged felé!
                """.trimIndent()
            }
        }
    }
}
