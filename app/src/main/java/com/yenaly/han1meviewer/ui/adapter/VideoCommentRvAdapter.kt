package com.yenaly.han1meviewer.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import coil.load
import coil.transform.CircleCropTransformation
import com.chad.library.adapter4.BaseDifferAdapter
import com.chad.library.adapter4.viewholder.DataBindingHolder
import com.google.android.material.button.MaterialButton
import com.itxca.spannablex.spannable
import com.lxj.xpopup.XPopup
import com.yenaly.han1meviewer.COMMENT_ID
import com.yenaly.han1meviewer.CSRF_TOKEN
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.databinding.ItemVideoCommentBinding
import com.yenaly.han1meviewer.logic.model.VideoComments
import com.yenaly.han1meviewer.ui.fragment.video.ChildCommentPopupFragment
import com.yenaly.han1meviewer.ui.fragment.video.CommentFragment
import com.yenaly.han1meviewer.ui.popup.ReplyPopup
import com.yenaly.han1meviewer.util.notNull
import com.yenaly.yenaly_libs.utils.makeBundle
import com.yenaly.yenaly_libs.utils.showShortToast

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/26 026 16:19
 */
class VideoCommentRvAdapter(private val fragment: Fragment? = null) :
    BaseDifferAdapter<VideoComments.VideoComment, VideoCommentRvAdapter.ViewHolder>(COMPARATOR) {

    init {
        isStateViewEnable = true
    }

    var replyPopup: ReplyPopup? = null

    companion object {
        private const val THUMB = 0

        val COMPARATOR = object : DiffUtil.ItemCallback<VideoComments.VideoComment>() {
            override fun areItemsTheSame(
                oldItem: VideoComments.VideoComment,
                newItem: VideoComments.VideoComment,
            ): Boolean {
                return oldItem.realReplyId == newItem.realReplyId
            }

            override fun areContentsTheSame(
                oldItem: VideoComments.VideoComment,
                newItem: VideoComments.VideoComment,
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: VideoComments.VideoComment,
                newItem: VideoComments.VideoComment,
            ): Any? {
                return if (oldItem.post.likeCommentStatus != newItem.post.likeCommentStatus ||
                    oldItem.post.unlikeCommentStatus != newItem.post.unlikeCommentStatus
                ) THUMB else null
            }
        }
    }

    inner class ViewHolder(view: View) : DataBindingHolder<ItemVideoCommentBinding>(view)

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        item: VideoComments.VideoComment?,
    ) {
        item.notNull()

        // 在release版中，主评论内容无法被复制，此为解决方法。
        holder.binding.tvContent.fixTextSelection()

        holder.binding.ivAvatar.load(item.avatar) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        holder.binding.tvContent.text = item.content
        holder.binding.tvDate.text = item.date
        holder.binding.tvUsername.text = item.username
        holder.binding.btnViewMoreReplies.isVisible = item.hasMoreReplies
        holder.binding.btnThumbUp.text = item.realLikesCount?.toString()
        holder.binding.btnThumbUp.setThumbUpIcon(item.post.likeCommentStatus)
        holder.binding.btnThumbDown.setThumbDownIcon(item.post.unlikeCommentStatus)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        item: VideoComments.VideoComment?,
        payloads: List<Any>,
    ) {
        if (payloads.isEmpty()) return super.onBindViewHolder(holder, position, item, payloads)
        item.notNull()
        if (payloads.first() == THUMB) {
            holder.binding.btnThumbUp.setThumbUpIcon(item.post.likeCommentStatus)
            holder.binding.btnThumbDown.setThumbDownIcon(item.post.unlikeCommentStatus)
            holder.binding.btnThumbUp.text = item.realLikesCount?.toString()
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        return ViewHolder(
            ItemVideoCommentBinding.inflate(
                LayoutInflater.from(context), parent, false
            ).root
        ).also { viewHolder ->
            viewHolder.binding.btnViewMoreReplies.setOnClickListener {
                val position = viewHolder.bindingAdapterPosition
                val item = getItem(position).notNull()
                check(fragment != null && fragment is CommentFragment)
                item.realReplyId.let { id ->
                    (ChildCommentPopupFragment().makeBundle(
                        COMMENT_ID to id,
                        CSRF_TOKEN to fragment.viewModel.csrfToken
                    ) as ChildCommentPopupFragment).showIn(context as FragmentActivity)
                }
            }
            viewHolder.binding.btnThumbUp.setOnClickListener {
                if (!Preferences.isAlreadyLogin) {
                    showShortToast(R.string.login_first)
                    return@setOnClickListener
                }
                val position = viewHolder.bindingAdapterPosition
                val item = getItem(position).notNull()

                if (item.isChildComment) {
                    check(fragment != null && fragment is ChildCommentPopupFragment)
                    fragment.viewModel.likeChildComment(
                        true, position, item,
                        likeCommentStatus = item.post.likeCommentStatus
                    )
                } else {
                    check(fragment != null && fragment is CommentFragment)
                    fragment.viewModel.likeComment(
                        true, position, item,
                        likeCommentStatus = item.post.likeCommentStatus
                    )
                }
            }
            viewHolder.binding.btnThumbDown.setOnClickListener {
                if (!Preferences.isAlreadyLogin) {
                    showShortToast(R.string.login_first)
                    return@setOnClickListener
                }
                val position = viewHolder.bindingAdapterPosition
                val item = getItem(position).notNull()
                if (item.isChildComment) {
                    check(fragment != null && fragment is ChildCommentPopupFragment)
                    fragment.viewModel.likeChildComment(
                        false, position, item,
                        unlikeCommentStatus = item.post.unlikeCommentStatus
                    )
                } else {
                    check(fragment != null && fragment is CommentFragment)
                    fragment.viewModel.likeComment(
                        false, position, item,
                        unlikeCommentStatus = item.post.unlikeCommentStatus
                    )
                }
            }
            viewHolder.binding.btnReply.setOnClickListener {
                if (!Preferences.isAlreadyLogin) {
                    showShortToast(R.string.login_first)
                    return@setOnClickListener
                }
                val position = viewHolder.bindingAdapterPosition
                val item = getItem(position).notNull()

                ReplyPopup(context).also { commentPopup ->
                    this.replyPopup = commentPopup
                    if (item.isChildComment) {
                        check(fragment != null && fragment is ChildCommentPopupFragment)
                        fragment.apply {
                            commentPopup.setOnSendListener {
                                viewModel.postReply(
                                    checkNotNull(commentId), commentPopup.comment
                                )
                            }
                        }
                        commentPopup.hint = context.getString(R.string.reply_child_comment)
                        commentPopup.initCommentPrefix(item.username)
                    } else {
                        check(fragment != null && fragment is CommentFragment)
                        fragment.apply {
                            commentPopup.setOnSendListener {
                                viewModel.postReply(
                                    item.realReplyId,
                                    commentPopup.comment
                                )
                            }
                        }
                        commentPopup.hint = spannable {
                            context.getString(R.string.reply).text()
                            "@${item.username}".span {
                                style(Typeface.BOLD)
                            }
                        }
                    }
                    XPopup.Builder(context).autoOpenSoftInput(true).asCustom(commentPopup).show()
                }
            }
        }
    }

    private fun MaterialButton.setThumbUpIcon(likeCommentStatus: Boolean) {
        if (likeCommentStatus) {
            setIconResource(R.drawable.ic_baseline_thumb_up_alt_24)
        } else {
            setIconResource(R.drawable.ic_baseline_thumb_up_off_alt_24)
        }
    }

    private fun MaterialButton.setThumbDownIcon(unlikeCommentStatus: Boolean) {
        if (unlikeCommentStatus) {
            setIconResource(R.drawable.ic_baseline_thumb_down_alt_24)
        } else {
            setIconResource(R.drawable.ic_baseline_thumb_down_off_alt_24)
        }
    }

    // stackoverflow-36801486
    private fun TextView.fixTextSelection() {
        setTextIsSelectable(false)
        post { setTextIsSelectable(true) }
    }
}