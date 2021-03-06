package com.example.webtoonservice.repository;

import com.example.webtoonservice.model.Fav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Collection;

@Repository
public interface FavRepository extends JpaRepository<Fav, Integer>{
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value="Delete from fav where tid=?1 and username=?2", nativeQuery=true)
    void deleteFav(Integer id, String user);

    
    @Query(value="Select * from fav where username=?1", nativeQuery=true)
    Collection<Fav> getFav(String user);

    @Query(value="Select * from fav where tid=?1 and username=?2", nativeQuery=true)
    Collection<Fav> getFavById(Integer tno, String user);
}