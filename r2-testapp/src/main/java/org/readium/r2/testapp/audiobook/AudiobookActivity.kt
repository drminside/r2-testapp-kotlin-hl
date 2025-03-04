package org.readium.r2.testapp.audiobook


import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_audiobook.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.readium.r2.navigator.audiobook.R2AudiobookActivity
import org.readium.r2.shared.Locations
import org.readium.r2.shared.LocatorText
import org.readium.r2.testapp.DRMManagementActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.Bookmark
import org.readium.r2.testapp.db.BookmarksDatabase
import org.readium.r2.testapp.outline.R2OutlineActivity


class AudiobookActivity : R2AudiobookActivity() {

    private lateinit var bookmarksDB: BookmarksDatabase
    private lateinit var progressDialog: ProgressDialog
    private var bookId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookmarksDB = BookmarksDatabase(this)
        progressDialog = indeterminateProgressDialog(getString(R.string.progress_wait_while_preparing_audiobook))

        Handler().postDelayed({
            bookId = intent.getLongExtra("bookId", -1)
            //Setting cover
            launch {
                if (intent.hasExtra("cover")) {
                    val byteArray = intent.getByteArrayExtra("cover")
                    val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
                    findViewById<ImageView>(R.id.imageView).setImageBitmap(bmp)
                }
                menuDrm?.isVisible = intent.getBooleanExtra("drm", false)
            }

        }, 100)

        mediaPlayer?.progress = progressDialog

    }

    private var menuDrm: MenuItem? = null
    private var menuToc: MenuItem? = null
    private var menuBmk: MenuItem? = null
    private var menuSettings: MenuItem? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_audio, menu)
        menuDrm = menu?.findItem(R.id.drm)
        menuToc = menu?.findItem(R.id.toc)
        menuBmk = menu?.findItem(R.id.bookmark)
        menuSettings = menu?.findItem(R.id.settings)

        menuSettings?.isVisible = false
        menuDrm?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.toc -> {
                val intent = Intent(this, R2OutlineActivity::class.java)
                intent.putExtra("publication", publication)
                intent.putExtra("bookId", bookId)
                startActivityForResult(intent, 2)
                return true
            }
            R.id.settings -> {
                // TODO do we need any settings ?
                return true
            }
            R.id.drm -> {
                startActivityForResult(intentFor<DRMManagementActivity>("publication" to publicationPath), 1)
                return true
            }
            R.id.bookmark -> {
                val resourceIndex = currentResource.toLong()

                val resource = publication.readingOrder[currentResource]
                val resourceHref = resource.href ?: ""
                val resourceType = resource.typeLink ?: ""
                val resourceTitle = resource.title ?: ""

                val bookmark = Bookmark(
                        bookId,
                        publicationIdentifier,
                        resourceIndex,
                        resourceHref,
                        resourceType,
                        resourceTitle,
                        Locations(progression = seekBar!!.progress.toDouble()),
                        LocatorText()
                )

                bookmarksDB.bookmarks.insert(bookmark)?.let {
                    launch {
                        toast("Bookmark added")
                    }
                } ?: run {
                    launch {
                        toast("Bookmark already exists")
                    }
                }

                return true
            }

            else -> return false
        }

    }

    override fun onStop() {
        super.onStop()
        progressDialog.dismiss()
    }

}




