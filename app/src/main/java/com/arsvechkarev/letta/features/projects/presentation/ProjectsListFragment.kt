package com.arsvechkarev.letta.features.projects.presentation

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.arsvechkarev.letta.LettaApplication
import com.arsvechkarev.letta.R
import com.arsvechkarev.letta.core.model.BackgroundType.Color
import com.arsvechkarev.letta.core.model.BackgroundType.DrawableRes
import com.arsvechkarev.letta.core.model.Project
import com.arsvechkarev.letta.core.mvp.MvpFragment
import com.arsvechkarev.letta.core.navigation.navigator
import com.arsvechkarev.letta.extensions.animateInvisibleAndScale
import com.arsvechkarev.letta.extensions.behavior
import com.arsvechkarev.letta.features.drawing.presentation.createColorArgs
import com.arsvechkarev.letta.features.drawing.presentation.createDrawableResArgs
import com.arsvechkarev.letta.features.drawing.presentation.createProjectArgs
import com.arsvechkarev.letta.features.projects.domain.ProjectsListRepository
import com.arsvechkarev.letta.features.projects.list.ProjectsListAdapter
import com.arsvechkarev.letta.views.behaviors.BottomSheetBehavior
import com.arsvechkarev.letta.views.behaviors.BottomSheetBehavior.State.SHOWN
import kotlinx.android.synthetic.main.fragment_projects_list.backgroundImageExample
import kotlinx.android.synthetic.main.fragment_projects_list.backgroundImagePalette
import kotlinx.android.synthetic.main.fragment_projects_list.backgroundImagesRecyclerView
import kotlinx.android.synthetic.main.fragment_projects_list.buttonNewProject
import kotlinx.android.synthetic.main.fragment_projects_list.createNewProjectButton
import kotlinx.android.synthetic.main.fragment_projects_list.dialogProjectBackground
import kotlinx.android.synthetic.main.fragment_projects_list.projectsListRoot
import kotlinx.android.synthetic.main.fragment_projects_list.projectsProgressBar
import kotlinx.android.synthetic.main.fragment_projects_list.recyclerAllProjects

class ProjectsListFragment : MvpFragment<ProjectsListView, ProjectsListPresenter>(
  ProjectsListPresenter::class, R.layout.fragment_projects_list
), ProjectsListView {
  
  private lateinit var chooseBgContainer: ChooseBgContainer
  
  private val adapter = ProjectsListAdapter(onProjectClick = { project ->
    navigator.openProject(createProjectArgs(
      project, projectsListRoot.width, projectsListRoot.height
    ))
  })
  
  override fun createPresenter(): ProjectsListPresenter {
    return ProjectsListPresenter(ProjectsListRepository(LettaApplication.appContext))
  }
  
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    recyclerAllProjects.adapter = adapter
    recyclerAllProjects.layoutManager = GridLayoutManager(requireContext(), 3,
      GridLayoutManager.VERTICAL, false)
    presenter.startLoadingProjects()
    prepareNewProjectDialog()
    chooseBgContainer = ChooseBgContainer(backgroundImageExample, backgroundImagePalette,
      backgroundImagesRecyclerView)
  }
  
  override fun onLoadedProjects(list: List<Project>) {
    adapter.submitList(list)
    projectsProgressBar.animateInvisibleAndScale()
  }
  
  override fun onProjectAdded(project: Project) {
    adapter.addProject(project)
  }
  
  override fun projectsAreEmpty() {
    projectsProgressBar.animateInvisibleAndScale()
  }
  
  override fun onBackPressed(): Boolean {
    val behavior = dialogProjectBackground.behavior<BottomSheetBehavior<*>>()
    if (behavior.state == SHOWN) {
      behavior.hide()
      return false
    }
    return true
  }
  
  override fun onDestroyView() {
    super.onDestroyView()
    recyclerAllProjects.adapter = null
  }
  
  private fun prepareNewProjectDialog() {
    val behavior = dialogProjectBackground.behavior<BottomSheetBehavior<*>>()
    buttonNewProject.setOnClickListener { behavior.show() }
    createNewProjectButton.setOnClickListener { openNewProject() }
  }
  
  private fun openNewProject() {
    val width = projectsListRoot.width
    val height = projectsListRoot.height
    val args = when (val type = chooseBgContainer.getBackgroundType()) {
      is Color -> createColorArgs(type.color, width, height)
      is DrawableRes -> createDrawableResArgs(type.drawableRes, width, height)
    }
    navigator.openProject(args)
  }
}