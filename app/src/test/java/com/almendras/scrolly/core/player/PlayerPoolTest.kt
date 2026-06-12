package com.almendras.scrolly.core.player

import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class PlayerPoolTest {

    private fun newPool(capacity: Int = 3) =
        PlayerPool(ApplicationProvider.getApplicationContext(), capacity = capacity)

    @Test
    fun `acquire devuelve la misma instancia para la misma pagina`() {
        val pool = newPool()
        val player = pool.acquire(0)

        assertSame(player, pool.acquire(0))
        pool.releaseAll()
    }

    @Test
    fun `paginas distintas reciben players distintos`() {
        val pool = newPool()

        assertNotSame(pool.acquire(0), pool.acquire(1))
        pool.releaseAll()
    }

    @Test
    fun `recycle devuelve el player al pool y se reutiliza`() {
        val pool = newPool(capacity = 1)
        val original = pool.acquire(0)

        pool.recycle(0)
        val reused = pool.acquire(5)

        assertSame(original, reused)
        pool.releaseAll()
    }

    @Test
    fun `al exceder la capacidad roba el player de la pagina mas lejana`() {
        val pool = newPool(capacity = 2)
        val player0 = pool.acquire(0)
        pool.acquire(1)

        // Pedimos la página 10: debe robar el de la página 0 (la más lejana)
        val player10 = pool.acquire(10)

        assertSame(player0, player10)
        assertNull(pool.playerFor(0))
        pool.releaseAll()
    }

    @Test
    fun `releaseAll limpia todas las asignaciones`() {
        val pool = newPool()
        pool.acquire(0)
        pool.acquire(1)

        pool.releaseAll()

        assertNull(pool.playerFor(0))
        assertNull(pool.playerFor(1))
    }
}
