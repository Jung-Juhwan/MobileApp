package com.example.teamtotest.fragment


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.teamtotest.R
import com.example.teamtotest.activity.NavigationbarActivity
import com.example.teamtotest.adapter.ProjectListAdapter
import com.example.teamtotest.dto.MembersDTO
import com.example.teamtotest.dto.MessageDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.bottombar_fragment2.view.*
import java.text.SimpleDateFormat
import java.util.*

class Frag2 : Fragment() {

    private var ProjectInfoList: ArrayList<HashMap<String, String>> =
        ArrayList<HashMap<String, String>>()
    var myProjectPIDlist: ArrayList<String> = ArrayList<String>()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var listener: ValueEventListener

    lateinit var mAdapter: ProjectListAdapter

    override fun onStart() {
        super.onStart()
        findMyProjectFromFirebaseDB()
        setListener_DataFromMyProjects()
    }

    override fun onStop() {
        databaseReference.removeEventListener(listener)
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.bottombar_fragment2, null)

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance()

        mAdapter =
            ProjectListAdapter(ProjectInfoList, requireActivity())
        //my_recycler_view.setAdapter(mAdapter)
        view.projectlist_recycler_view.adapter = mAdapter
        mAdapter.notifyDataSetChanged()



        view.add_button.setOnClickListener {
//            val intent = Intent(context, AddProjectActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            startActivity(intent)

            (requireActivity() as NavigationbarActivity).toProject()
        }

        return view
    }

    private fun findMyProjectFromFirebaseDB() {
        myProjectPIDlist.clear()
        val myUID = firebaseAuth.getCurrentUser()!!.getUid()

        databaseReference = firebaseDatabase.getReference("ProjectList")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // 각각 프로젝트별로, 멤버중에 나 자신이 있는지 확인.
                for (snapshot in dataSnapshot.children) {
                    //ProjectInfo.put("PID", snapshot.getKey());
                    val membersPerProject = snapshot.child("members")
                        .getValue(MembersDTO::class.java) // memberUID 정보를 가져옴.

                    for (i in 0 until membersPerProject!!.UID_list!!.size) {
                        if (membersPerProject!!.UID_list!![i].equals(myUID)) {
                            myProjectPIDlist.add(snapshot.key.toString())
                            Log.d("Find my PID ! ---> ", snapshot.key!!)
                        }
                    }
                }
                //                setListener_DataFromMyProjects();
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("ExtraUserInfoActivity", "loadPost:onCancelled", databaseError.toException())
            }
        })

    }

    private fun setListener_DataFromMyProjects() {
        // private HashMap<String, String> ProjectInfo;
        listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // 각각 프로젝트별로, 멤버중에 나 자신이 있는지 확인.
                ProjectInfoList.clear()    // 갱신될 때 이미 있던 데이터는 날리기
                val myUID : String = firebaseAuth!!.currentUser!!.uid
                var readCnt : Int = 0
                for (snapshot in dataSnapshot.children) {
                    for (i in myProjectPIDlist.indices) {
                        if (myProjectPIDlist.get(i) == snapshot.key) { // 내 프로젝트면 ~
                            val projectInfo = HashMap<String, String>()

                            projectInfo["PID"] = snapshot.key.toString()
                            projectInfo["projectName"] = snapshot.child("projectName").value!!.toString()

                            val membersDTO =
                                snapshot.child("members").getValue(MembersDTO::class.java)
                            projectInfo["howManyMembers"] = membersDTO!!.UID_list!!.size.toString()

                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            var date : Date?= null
                            var latestmessageDTO : MessageDTO?=null

                            for(messageSnapshot : DataSnapshot in snapshot.child("messageList").children){  // 최신메세지 찾기 / 안읽은 메세지 카운트
//                                Log.d("Date", messageSnapshot.key)
//                                Toast.makeText(requireActivity(), messageSnapshot.key, Toast.LENGTH_SHORT).show()
                                var tmp = dateFormat.parse(messageSnapshot.key)
//                                Log.e("TAG", tmp.toString())
                                if(date==null || date < tmp){
                                    date = tmp
//                                    Log.e("It is latest!", date.toString())
                                }

                                val messageDTO = messageSnapshot.getValue(MessageDTO::class.java)  // 데이터를 가져와서
                                if(!messageDTO!!.read!!.contains(myUID)) { // 내 uid가 없으면 count!
                                    readCnt++
                                }
                            }
                            projectInfo["noReadMessageCount"] = readCnt.toString()
                            readCnt=0

                            for(messageSnapshot : DataSnapshot in snapshot.child("messageList").children){
                                if(date== dateFormat.parse(messageSnapshot.key)){
                                    latestmessageDTO = messageSnapshot.getValue(MessageDTO::class.java)!!
                                }
                            }
                            if(latestmessageDTO!=null){
                                projectInfo["lastMessage"] = latestmessageDTO!!.message
                                projectInfo["lastMessageSentTime"] = date.toString().substring(11, 16)
                            }else{
                                projectInfo["lastMessage"] = ""
                                projectInfo["lastMessageSentTime"] = ""
                            }

                            ProjectInfoList.add(projectInfo)   // 데이터들을 담아서 list에 넣을 데이터를 담을 infolist에다가 넣는다!
                        }
                    }
                }
                mAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("ExtraUserInfoActivity", "loadPost:onCancelled", databaseError.toException())
            }
        }
        databaseReference = firebaseDatabase.getReference("ProjectList")
        databaseReference.addValueEventListener(listener)       // Projectlist 경로에 있는 데이터가 뭔가가 바뀌면 알려주는 listener 설정!
    }

}