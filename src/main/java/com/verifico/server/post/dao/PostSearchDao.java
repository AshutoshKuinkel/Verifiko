package com.verifico.server.post.dao;

// handle filtering logic here, and inject directly into post
// service. So both post repo and post search dao are in service.
// In getAllPosts(), check if search param exists:
// If yes → call postSearchDao.searchPosts(...)
// If no → call postRepository.findAll...() 
public class PostSearchDao {

}
