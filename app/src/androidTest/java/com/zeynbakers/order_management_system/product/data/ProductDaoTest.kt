package com.zeynbakers.order_management_system.product.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zeynbakers.order_management_system.core.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProductDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.productDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetProduct() = runBlocking {
        val product = ProductEntity(
            name = "Croissant",
            defaultPrice = BigDecimal("150.00"),
            emoji = "🥐"
        )
        val id = dao.insertProduct(product)

        val result = dao.getProductById(id)
        assertNotNull(result)
        assertEquals("Croissant", result?.name)
        assertEquals(0, BigDecimal("150.00").compareTo(result?.defaultPrice))
    }

    @Test
    fun archiveProduct_hidesFromActiveList() = runBlocking {
        val p1 = ProductEntity(name = "Active", defaultPrice = BigDecimal.TEN, emoji = "A")
        val p2 = ProductEntity(name = "To Archive", defaultPrice = BigDecimal.TEN, emoji = "B")

        val id1 = dao.insertProduct(p1)
        val id2 = dao.insertProduct(p2)

        var active = dao.getActiveProducts()
        assertEquals(2, active.size)

        dao.archiveProduct(id2)

        active = dao.getActiveProducts()
        assertEquals(1, active.size)
        assertEquals(id1, active[0].id)

        val archived = dao.getProductById(id2)
        assertTrue(archived?.archived == true)
    }

    @Test
    fun searchActiveProducts_filtersCorrectly() = runBlocking {
        dao.insertProduct(
            ProductEntity(
                name = "Apple Pie",
                defaultPrice = BigDecimal.ONE,
                emoji = "🍎"
            )
        )
        dao.insertProduct(
            ProductEntity(
                name = "Banana Bread",
                defaultPrice = BigDecimal.ONE,
                emoji = "🍌"
            )
        )
        dao.insertProduct(
            ProductEntity(
                name = "Cherry Cake",
                defaultPrice = BigDecimal.ONE,
                emoji = "🍒"
            )
        )

        val results = dao.searchActiveProducts("%pie%")
        assertEquals(1, results.size)
        assertEquals("Apple Pie", results[0].name)
    }

    @Test
    fun getAllProductsFlow_updatesOnInsert() = runBlocking {
        val flow = dao.getAllProductsFlow()

        assertTrue(flow.first().isEmpty())

        dao.insertProduct(ProductEntity(name = "New", defaultPrice = BigDecimal.ONE, emoji = "N"))

        val list = flow.first()
        assertEquals(1, list.size)
        assertEquals("New", list[0].name)
    }
}
