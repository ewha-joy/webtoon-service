package com.example.webtoonservice.controller;


import com.example.webtoonservice.exception.FileStorageException;
import com.example.webtoonservice.exception.ResourceNotFoundException;
import com.example.webtoonservice.model.*;
import com.example.webtoonservice.repository.*;
import com.example.webtoonservice.payload.*;
import com.example.webtoonservice.security.JwtTokenProvider;
import com.google.api.client.json.Json;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobTargetOption;
import com.google.cloud.storage.Storage.PredefinedAcl;
import com.google.cloud.storage.StorageOptions;
import feign.FeignException;
import io.jsonwebtoken.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@RequestMapping("/webtoon-service")
@RestController
public class ToonController {

    private static final Logger logger = LoggerFactory.getLogger(ToonController.class);

    @Autowired
    private ToonRepository toonRepository;
    
    @Autowired
    private EpisodeRepository episodeRepository;


    @Autowired
    private ToonThumbnailRepository toonThumbnailRepository;

    @Autowired
    private EpiThumbnailRepository epiThumbnailRepository;

    @Autowired
    private EpiToonRepository epiToonRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RateRepository rateRepository;

    @Autowired
    private FavRepository favRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private Environment env;


    @Autowired
    private RentToonClient rentToonClient;


    // private static Storage storage = StorageOptions.getDefaultInstance().getService();



    // ??? ?????? ??????
    @PostMapping(value = "/newAdd", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Toon newAdd(@RequestParam("title") String title, @RequestParam("artist") String artist,
            @RequestParam("day") String day, @RequestParam("genre") String genre,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {


            String bearerToken = request.getHeader("Authorization");
            String jwt = bearerToken.substring(7, bearerToken.length());
            Long userId = tokenProvider.getUserIdFromJWT(jwt);
            Toon toon = new Toon(title, artist, day, genre, userId);
            ToonThumbnail toonThumbnail = this.uploadsSave(file);
            toon.setToonThumbnail(toonThumbnail);
            toonThumbnail.setToon(toon);
            Toon result = toonRepository.save(toon);
            return result;
    }

    // ??????????????? ?????????
    @PostMapping(value="/uploads")
    public ToonThumbnail uploadsSave(MultipartFile file) {
            // ?????? ?????? -> UUID
            UUID uid = UUID.randomUUID();
            String filename = uid.toString() + file.getContentType().replace('/','.').toString();
            String fileName = StringUtils.cleanPath(filename);

            try {
                String keyFileName = "oidc-project-317910-8a43df642d7f.json";
                InputStream keyFile = ResourceUtils.getURL("classpath:" + keyFileName).openStream();
                Storage storage = StorageOptions.newBuilder()
                        // Key ?????? ?????? ??????
                        .setCredentials(GoogleCredentials.fromStream(keyFile))
                        .build().getService();
                BlobInfo blobInfo = storage.create(
                        BlobInfo.newBuilder("oidc-file-storage", "uploads/"+fileName).build(), //get original file name
                        file.getBytes(), // the file
                        BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ) // Set file permission
                );
                String fileUri = "https://storage.googleapis.com/oidc-file-storage/uploads/" + filename;


                ToonThumbnail toonThumbnail = new ToonThumbnail(fileName, file.getContentType(), fileUri, file.getSize());
                return toonThumbnail;

            } catch (Exception ex) {
                throw new FileStorageException("Could not store file " +fileName + ". Please try again!", ex);
            }
    }


    // ??????????????? ?????????
    @PostMapping(value="/thumbnails")
    public EpiThumbnail thumbnailSave(MultipartFile file) {
        // ?????? ?????? -> UUID
        UUID uid = UUID.randomUUID();
        String filename = uid.toString() + file.getContentType().replace('/','.').toString();
        String fileName = StringUtils.cleanPath(filename);

        try {
            String keyFileName = "oidc-project-317910-8a43df642d7f.json";
            InputStream keyFile = ResourceUtils.getURL("classpath:" + keyFileName).openStream();
            Storage storage = StorageOptions.newBuilder()
                    // Key ?????? ?????? ??????
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build().getService();
            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder("oidc-file-storage", "thumbnails/"+fileName).build(), //get original file name
                    file.getBytes(), // the file
                    BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ) // Set file permission
            );
            String fileUri = "https://storage.googleapis.com/oidc-file-storage/thumbnails/" + filename;

            EpiThumbnail epiThumbnail = new EpiThumbnail(fileName, file.getContentType(), fileUri, file.getSize());
            return epiThumbnail;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " +fileName + ". Please try again!", ex);
        }
    }


    // ??????????????? ?????????
    @PostMapping(value="/toons")
    public EpiToon ToonSave(MultipartFile file) {
        // ?????? ?????? -> UUID
        UUID uid = UUID.randomUUID();
        String filename = uid.toString() + file.getContentType().replace('/','.').toString();
        String fileName = StringUtils.cleanPath(filename);

        try {
            String keyFileName = "oidc-project-317910-8a43df642d7f.json";
            InputStream keyFile = ResourceUtils.getURL("classpath:" + keyFileName).openStream();
            Storage storage = StorageOptions.newBuilder()
                    // Key ?????? ?????? ??????
                    .setCredentials(GoogleCredentials.fromStream(keyFile))
                    .build().getService();
            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder("oidc-file-storage", "toons/"+fileName).build(), //get original file name
                    file.getBytes(), // the file
                    BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ) // Set file permission
            );
            String fileUri = "https://storage.googleapis.com/oidc-file-storage/toons/" + filename;

            EpiToon epiToon = new EpiToon(fileName, file.getContentType(), fileUri, file.getSize());
            return epiToon;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " +fileName + ". Please try again!", ex);
        }
    }


    // ??? ???????????? ??????
    @PostMapping(value = "/newEpi", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Episode newEpi(@RequestParam("epiTitle") String epiTitle, @RequestParam("toonId") Toon toon,
            @RequestParam("eFile") MultipartFile eFile, @RequestParam("mFile") MultipartFile mFile) {
        Episode episode = new Episode(epiTitle, toon);
        EpiThumbnail epiThumbnail = this.thumbnailSave(eFile);
        EpiToon epiToon = this.ToonSave(mFile);
        episode.setEpiToon(epiToon);
        epiToon.setEpisode(episode);
        episode.setEpiThumbnail(epiThumbnail);
        epiThumbnail.setEpisode(episode);
        Episode result = episodeRepository.save(episode);
        return result;

    }



    // ??? ????????? ??????
    @PostMapping("/saveComment/{id}")
    public Episode createComment(@PathVariable int id, @RequestParam("user") String user, @RequestParam("comment") String comment) {
        Episode epi = episodeRepository.findById(id).get();
        Comment com = new Comment(user, comment);
        com.setEpisode(epi);
        
        epi.getComments().add(com);
        return episodeRepository.save(epi);
    }



    // ???????????? ??????
    @PostMapping("/saveFav/{id}")
    public Toon saveFav(@PathVariable int id, @RequestParam("user") String user, @RequestParam("title")String title, @RequestParam("webtoonId")Integer webtoonId){
        Toon toon = toonRepository.findById(id).get();
        Fav fav = new Fav(user, title, toon);
        toon.getFav().add(fav);
        return toonRepository.save(toon);
    }



    // ?????? webtoonHome?????? ???????????? ??????
    @DeleteMapping("/deleteFav/{id}/{user}")
    public void deleteFav(@PathVariable("id") int id, @PathVariable("user") String user){
        favRepository.deleteFav(id, user);
    }


    // ???????????? ??????
    @DeleteMapping("/deleteFavById/{id}")
    public void defeFavById(@PathVariable("id") int id){
        favRepository.deleteById(id);
    }

    // ???????????? ????????????
    @GetMapping("/getFav/{user}")
    public Collection<Fav> getFav(@PathVariable("user") String user){
        System.out.println(favRepository.getFav(user));
        return favRepository.getFav(user);
    }

    // ????????? ?????? ???????????? ??????
    @GetMapping("/getFavById/{tno}/{user}")
    public Collection<Fav> getFav(@PathVariable("tno") Integer tno, @PathVariable("user") String user){
        return favRepository.getFavById(tno, user);
    }

    //????????? ?????? ?????????
    @PutMapping("/uploadEditComment/{id}")
    public Comment uploadEditComment(@PathVariable int id, @RequestParam("comment") String comment){
        Comment com = commentRepository.findById(id).get();
        com.setComment(comment);
        return commentRepository.save(com);
    }

    // Rate ??????
    @PostMapping("/uploadRate/{id}")
    public Episode uploadRate(@PathVariable int id, @RequestParam("user") String user, @RequestParam("rate") Integer rate){
        Episode epi = episodeRepository.findById(id).get();
        Rate r = new Rate(user, rate);
        r.setEpisode(epi);
        epi.getRate().add(r);
        return episodeRepository.save(epi);
    }

    // ?????? Rate ????????????
    @GetMapping(value={"/fetchRate/{id}/{user}"})
    public Optional<Rate> fetchRate(@PathVariable("id") int id, @PathVariable("user") String user){
        return rateRepository.getRateByEpiId(id, user);
    }

    // Rate ??????
    @PutMapping("/uploadEditRate/{id}")
    public Rate uploadEditRate(@PathVariable int id,@RequestParam("user") String user, @RequestParam("rate") Integer rate){
        Rate r = rateRepository.getRateByEpiId(id, user).get();
        r.setRate(rate);
        return rateRepository.save(r);
    }


    // ??? ???????????? ????????? ?????? webtoonId ??? ????????????
    @GetMapping("/getToonIdAndName")
    public List<Map<String, Object>> getTIAN() {
        return toonRepository.getToonIdAndName();
    }


    // ????????? ????????????(id??? ??????) ????????????
    @GetMapping("/getToonIdAndName/me")
    public List<Map<String, Object>> getTIANm(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7, bearerToken.length());
        Long userId = tokenProvider.getUserIdFromJWT(jwt);
        return toonRepository.getToonIdAndNameByUser(userId);
    }


    // ?????? ?????? + ???????????? ?????? + ?????? ?????? + ?????? ?????? + ????????? ??????
    @GetMapping("/getToon")
    public Collection<Toon> getToon() {
        return toonRepository.findAll();
    }


    //????????? ?????? ????????? find
    @GetMapping("/getToon/me")
    public Collection<Toon> getToonme(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7, bearerToken.length());
        Long userId = tokenProvider.getUserIdFromJWT(jwt);
        return toonRepository.findByUser(userId);
    }

    // ??????id??? ???????????? ?????? ??????
    @GetMapping("/getEpi/{id}")
    public Collection<Episode> getEpi(@PathVariable int id) {
        return episodeRepository.getEpi(id);
    }

    // ??????id??? ?????? ?????? ??????
    @GetMapping("/getComment/{id}")
    public Collection<Comment> getComment(@PathVariable int id) {
        return commentRepository.getComment(id);
    }


    // ???????????? id??? ?????? ???????????? ????????????
    // ?????? ???????????? ?????? ????????? ???????????? check!
    @GetMapping("/getEpiById/{id}")
    public Optional<Episode> getEpiById(@PathVariable int id, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String token = request.getHeader("Authorization");
        /*Using FeignClient*/
        /*Error Decoder*/
            String result = rentToonClient.getRentToonResult(Integer.toString(id), token);
            if(result.equals("false")) return null;
            else return episodeRepository.findById(id);

    }



    // ?????? id ??? ?????? ?????????????????? ????????????
    @GetMapping("/getToonById/{id}")
    public Optional<Toon> getToonById(@PathVariable int id) {
        return toonRepository.findById(id);
    }

    //??????????????? id??? ????????????????????? ????????????
    @GetMapping("/getToonThumbnailById/{id}")
    public Optional<ToonThumbnail> getToonThumbnailById(@PathVariable int id) {
        return toonThumbnailRepository.getToonThumbnailByID(id);
    }



    // ????????? ??????
    @DeleteMapping("/deleteToonThumbnail/{id}")
    public void deleteToonThumbnail(@PathVariable Integer id) {
        toonThumbnailRepository.deleteToonThumbnail(id);
    }

    // ?????? ?????? ??????
    @DeleteMapping("/deleteToon/{id}")
    public void deleteToon(@PathVariable Integer id) {
        toonRepository.deleteById(id);
    }

    // ?????? ????????? ??????
    @DeleteMapping("/deleteComment/{id}")
    public void deleteCommen(@PathVariable Integer id){
        commentRepository.deleteById(id);
    }

    //?????? ???????????? ??????
    @DeleteMapping("/deleteEpi/{id}")
    public void deleteEpi(@PathVariable Integer id) {
        episodeRepository.deleteById(id);
    }

    //?????? ???????????? ????????? ?????? ??? ???????????? ????????????
    @GetMapping("/getEditEpi/{id}")
    public episodeEpiPayload getEditEpiById(@PathVariable int id){
        Episode epi = episodeRepository.findById(id).orElse(null);
        episodeEpiPayload epiinfo = new episodeEpiPayload();
        if(epi!=null){
            epiinfo.setEpiTitle(epi.getEpiTitle());
            epiinfo.setEno(epi.getEno());
            epiinfo.setWebtoonId(epi.getToon().getTno());
            epiinfo.setWebtoonTitle(epi.getToon().getTitle());
        }
        return epiinfo;
    }

    //???????????? ?????? ??? ?????? ????????? ????????????
    @GetMapping("/getToonTitle/{id}")
    public Optional<Toon> getToonTitle(@PathVariable int id){
        return toonRepository.findById(id);
    }

    


    // ????????? ?????? ?????????
    @PutMapping(value = "/uploadEditToon/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Toon uploadEditToon(@PathVariable int id, @RequestParam("title") String title, @RequestParam("artist") String artist,
            @RequestParam("day") String day, @RequestParam("genre") String genre,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7, bearerToken.length());
        Long userId = tokenProvider.getUserIdFromJWT(jwt);

        Toon toon = toonRepository.findById(id).get();
        toon.setTitle(title);
        toon.setArtist(artist);
        toon.setDay(day);
        toon.setGenre(genre);
        toon.setUser_no(userId);

        ToonThumbnail toonThumbnail = this.uploadsSave(file);
        
        toon.setToonThumbnail(toonThumbnail);

        toonThumbnail.setToon(toon);

        Toon result = toonRepository.save(toon);

        return result;

    }

    // ????????? ?????? ????????? (?????? ????????? ????????? ???)
    @PutMapping(value = "/uploadEditToonExceptFile/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Toon uploadEditToonExceptFile(@PathVariable int id, @RequestParam("title") String title, @RequestParam("artist") String artist,
            @RequestParam("day") String day, @RequestParam("genre") String genre,  HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7, bearerToken.length());
        Long userId = tokenProvider.getUserIdFromJWT(jwt);

        Toon toon = toonRepository.findById(id).get();
        toon.setTitle(title);
        toon.setArtist(artist);
        toon.setDay(day);
        toon.setGenre(genre);
        toon.setUser_no(userId);

        Toon result = toonRepository.save(toon);

        return result;

    }



    // ????????? ???????????? ?????????
    @PutMapping(value = "/uploadEditEpi/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Episode uploadEditEpi(@PathVariable int id, @RequestParam("epiTitle") String epiTitle,
            @RequestParam("eFile") MultipartFile eFile, @RequestParam("mFile") MultipartFile mFile) {
        

        Episode episode = episodeRepository.findById(id).get();
        episode.setEpiTitle(epiTitle);

        EpiThumbnail epiThumbnail = this.thumbnailSave(eFile);
        EpiToon epiToon = this.ToonSave(mFile);

        
        episode.setEpiToon(epiToon);
        epiToon.setEpisode(episode);

        episode.setEpiThumbnail(epiThumbnail);
        epiThumbnail.setEpisode(episode);

        Episode result = episodeRepository.save(episode);

        return result;

    }

    // ????????? ???????????? ????????? (????????? ???????????? ??????)
    @PutMapping(value = "/uploadEditEpiExceptTaM/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Episode uploadEditEpiExceptTaM(@PathVariable int id, @RequestParam("epiTitle") String epiTitle) {
        

        Episode episode = episodeRepository.findById(id).get();
        episode.setEpiTitle(epiTitle);

        Episode result = episodeRepository.save(episode);

        return result;

    }

    // ????????? ???????????? ?????????(???????????? ???????????? ??????)
    @PutMapping(value = "/uploadEditEpiExceptM/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Episode uploadEditEpiExceptM(@PathVariable int id, @RequestParam("epiTitle") String epiTitle,
            @RequestParam("eFile") MultipartFile eFile) {
        

        Episode episode = episodeRepository.findById(id).get();
        episode.setEpiTitle(epiTitle);

        EpiThumbnail epiThumbnail = this.thumbnailSave(eFile);

        episode.setEpiThumbnail(epiThumbnail);
        epiThumbnail.setEpisode(episode);

        Episode result = episodeRepository.save(episode);

        return result;

    }

    // ????????? ???????????? ?????????(????????? ???????????? ??????)
    @PutMapping(value = "/uploadEditEpiExceptT/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public Episode uploadEditEpiExceptT(@PathVariable int id, @RequestParam("epiTitle") String epiTitle,
            @RequestParam("mFile") MultipartFile mFile) {
        

        Episode episode = episodeRepository.findById(id).get();
        episode.setEpiTitle(epiTitle);
        EpiToon epiToon = this.ToonSave(mFile);
        episode.setEpiToon(epiToon);
        epiToon.setEpisode(episode);

        Episode result = episodeRepository.save(episode);

        return result;

    }



    @GetMapping("/getEpiThumbnailById/{id}")
    public Optional<EpiThumbnail> getEpiThumbnailById(@PathVariable int id) {
        return epiThumbnailRepository.getEpiThumbnailById(id);
    }

    @DeleteMapping("/deleteEpiThumbnail/{id}")
    public void deleteEpiThumbnail(@PathVariable Integer id) {
        epiThumbnailRepository.deleteEpiThumbnail(id);
    }

    @DeleteMapping("/deleteEpiToon/{id}")
    public void deleteEpiToon(@PathVariable Integer id) {
        epiToonRepository.deleteEpiToon(id);
    }

    @GetMapping("/getEpiToon/{id}")
    public Optional<EpiToon> getEpiToon(@PathVariable int id) {
        return epiToonRepository.getEpiToon(id);
    }

    // ????????????????????????
    @GetMapping("/getAvgRate/{id}")
    public Double getAvgRate(@PathVariable int id){
        return rateRepository.getAvgRate(id);
    }

    
}
