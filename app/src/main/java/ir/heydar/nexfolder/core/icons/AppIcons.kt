package ir.heydar.nexfolder.core.icons

import androidx.annotation.DrawableRes
import ir.heydar.nexfolder.R

/**
 * Central registry for all custom drawable resources.
 * All icon references should go through this object.
 */
object AppIcons {

    // === File Icons ===
    object File {
        @DrawableRes val generic = R.drawable.ic_file
        @DrawableRes val image = R.drawable.ic_file_image
        @DrawableRes val video = R.drawable.ic_file_video
        @DrawableRes val audio = R.drawable.ic_file_audio
        @DrawableRes val document = R.drawable.ic_file_document
        @DrawableRes val archive = R.drawable.ic_file_archive
        @DrawableRes val apk = R.drawable.ic_file_apk
        @DrawableRes val text = R.drawable.ic_file_text
        @DrawableRes val unknown = R.drawable.ic_file_unknown

        /** Format-family icons that go beyond the eight generic file types. */
        object Format {
            @DrawableRes val pdf = R.drawable.ic_file_pdf
            @DrawableRes val word = R.drawable.ic_file_word
            @DrawableRes val spreadsheet = R.drawable.ic_file_spreadsheet
            @DrawableRes val presentation = R.drawable.ic_file_presentation
            @DrawableRes val text = R.drawable.ic_file_text
            @DrawableRes val ebook = R.drawable.ic_file_ebook
            @DrawableRes val font = R.drawable.ic_file_font
            @DrawableRes val database = R.drawable.ic_file_database
            @DrawableRes val config = R.drawable.ic_file_config
            @DrawableRes val json = R.drawable.ic_file_json
            @DrawableRes val code = R.drawable.ic_file_code
            @DrawableRes val script = R.drawable.ic_file_script
            @DrawableRes val markdown = R.drawable.ic_file_markdown
            @DrawableRes val web = R.drawable.ic_file_web
            @DrawableRes val email = R.drawable.ic_file_email
            @DrawableRes val calendar = R.drawable.ic_file_calendar
            @DrawableRes val contact = R.drawable.ic_file_contact
            @DrawableRes val playlist = R.drawable.ic_file_playlist
            @DrawableRes val torrent = R.drawable.ic_file_torrent
            @DrawableRes val map = R.drawable.ic_file_map
            @DrawableRes val model3d = R.drawable.ic_file_model3d
            @DrawableRes val cad = R.drawable.ic_file_cad
            @DrawableRes val design = R.drawable.ic_file_design
            @DrawableRes val disk = R.drawable.ic_file_disk
            @DrawableRes val executable = R.drawable.ic_file_executable
            @DrawableRes val library = R.drawable.ic_file_library
            @DrawableRes val binary = R.drawable.ic_file_binary
            @DrawableRes val packageFile = R.drawable.ic_file_package
            @DrawableRes val link = R.drawable.ic_file_link
            @DrawableRes val backup = R.drawable.ic_file_backup

            object Audio {
                @DrawableRes val mp3 = R.drawable.ic_file_audio_mp3
                @DrawableRes val wav = R.drawable.ic_file_audio_wav
                @DrawableRes val flac = R.drawable.ic_file_audio_flac
                @DrawableRes val aac = R.drawable.ic_file_audio_aac
                @DrawableRes val ogg = R.drawable.ic_file_audio_ogg
                @DrawableRes val m4a = R.drawable.ic_file_audio_m4a
                @DrawableRes val wma = R.drawable.ic_file_audio_wma
                @DrawableRes val mid = R.drawable.ic_file_audio_mid
            }

            object Archive {
                @DrawableRes val zip = R.drawable.ic_file_archive_zip
                @DrawableRes val rar = R.drawable.ic_file_archive_rar
                @DrawableRes val sevenZip = R.drawable.ic_file_archive_7z
                @DrawableRes val tar = R.drawable.ic_file_archive_tar
                @DrawableRes val gz = R.drawable.ic_file_archive_gz
            }
        }
        
        object Video {
            @DrawableRes val mp4 = R.drawable.ic_file_video_mp4
            @DrawableRes val mkv = R.drawable.ic_file_video_mkv
            @DrawableRes val avi = R.drawable.ic_file_video_avi
            @DrawableRes val mov = R.drawable.ic_file_video_mov
            @DrawableRes val webm = R.drawable.ic_file_video_webm
            @DrawableRes val gp3 = R.drawable.ic_file_video_3gp
            @DrawableRes val flv = R.drawable.ic_file_video_flv
            @DrawableRes val mpeg = R.drawable.ic_file_video_mpeg
            @DrawableRes val mpg = R.drawable.ic_file_video_mpg
            @DrawableRes val m4v = R.drawable.ic_file_video_m4v
            @DrawableRes val ts = R.drawable.ic_file_video_ts
        }
        
        object Image {
            @DrawableRes val jpg = R.drawable.ic_file_image_jpg
            @DrawableRes val jpeg = R.drawable.ic_file_image_jpeg
            @DrawableRes val png = R.drawable.ic_file_image_png
            @DrawableRes val webp = R.drawable.ic_file_image_webp
            @DrawableRes val gif = R.drawable.ic_file_image_gif
            @DrawableRes val bmp = R.drawable.ic_file_image_bmp
            @DrawableRes val heic = R.drawable.ic_file_image_heic
            @DrawableRes val heif = R.drawable.ic_file_image_heif
            @DrawableRes val svg = R.drawable.ic_file_image_svg
            @DrawableRes val tiff = R.drawable.ic_file_image_tiff
            @DrawableRes val tif = R.drawable.ic_file_image_tif
            @DrawableRes val ico = R.drawable.ic_file_image_ico
            @DrawableRes val avif = R.drawable.ic_file_image_avif
        }
    }

    // === Folder Icons ===
    object Folder {
        @DrawableRes val default = R.drawable.ic_folder
        @DrawableRes val download = R.drawable.ic_folder_download
        @DrawableRes val documents = R.drawable.ic_folder_documents
        @DrawableRes val pictures = R.drawable.ic_folder_pictures
        @DrawableRes val music = R.drawable.ic_folder_music
        @DrawableRes val movies = R.drawable.ic_folder_movies
        @DrawableRes val dcim = R.drawable.ic_folder_dcim
        @DrawableRes val android = R.drawable.ic_folder_android
    }

    // === Action Icons ===
    object Action {
        @DrawableRes val copy = R.drawable.ic_action_copy
        @DrawableRes val move = R.drawable.ic_action_move
        @DrawableRes val rename = R.drawable.ic_action_rename
        @DrawableRes val delete = R.drawable.ic_action_delete
        @DrawableRes val share = R.drawable.ic_action_share
        @DrawableRes val search = R.drawable.ic_action_search
        @DrawableRes val sort = R.drawable.ic_action_sort
        @DrawableRes val more = R.drawable.ic_action_more
    }

    // === Navigation Icons ===
    object Nav {
        @DrawableRes val back = R.drawable.ic_nav_back
        @DrawableRes val forward = R.drawable.ic_nav_forward
        @DrawableRes val up = R.drawable.ic_nav_up
        @DrawableRes val home = R.drawable.ic_nav_home
        @DrawableRes val close = R.drawable.ic_nav_close
        @DrawableRes val menu = R.drawable.ic_nav_menu
    }

    // === Storage Icons ===
    object Storage {
        @DrawableRes val internal = R.drawable.ic_storage_internal
        @DrawableRes val sdCard = R.drawable.ic_storage_sd_card
        @DrawableRes val usb = R.drawable.ic_storage_usb
    }

    // === Status Icons ===
    object Status {
        @DrawableRes val empty = R.drawable.ic_folder
        @DrawableRes val error = R.drawable.ic_file_unknown
        @DrawableRes val noResults = R.drawable.ic_action_search
        @DrawableRes val permission = R.drawable.ic_file_android
    }

    // === Category Icons ===
    object Category {
        @DrawableRes val image = R.drawable.ic_file_image
        @DrawableRes val video = R.drawable.ic_file_video
        @DrawableRes val audio = R.drawable.ic_file_audio
        @DrawableRes val document = R.drawable.ic_file_document
        @DrawableRes val download = R.drawable.ic_folder_download
        @DrawableRes val archive = R.drawable.ic_file_archive
        @DrawableRes val apk = R.drawable.ic_file_apk
    }
}
