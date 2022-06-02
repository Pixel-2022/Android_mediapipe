import cv2
import mediapipe as mp
import numpy as np
import time, os
import itertools

mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles
mp_holistic = mp.solutions.holistic


actions=['1']
seq_length=30
secs_for_action=60
start_frame = 39
  
vidio=cv2.VideoCapture('./dataset/MOV000247604_700X466.mp4')

vidio.set(cv2.CAP_PROP_POS_FRAMES,start_frame)


result=[]



LEFT_EYE_INDEXES = list(set(itertools.chain(*mp.solutions.face_mesh.FACEMESH_LEFT_EYE)))
RIGHT_EYE_INDEXES = list(set(itertools.chain(*mp.solutions.face_mesh.FACEMESH_RIGHT_EYE)))
MOUTH=[ 78 ,  191 ,  80 ,  81 ,  82 ,  13 ,  312 ,  311 ,  310 ,  415 ,  308, 95 ,  88 ,  178 ,  87 ,  14 ,  317 ,  402 ,  318 ,  324]
POSE=[11,12,13,14,15,16,23,24,25,26]

for idx, action in enumerate(actions):

  ret, img=vidio.read()

  cv2.waitKey(1000)
  num=0
  start_time = time.time()

  while time.time() - start_time < secs_for_action:
    ret, img = vidio.read()
    

    with mp_holistic.Holistic(
        static_image_mode=True,
        model_complexity=2,
        enable_segmentation=True,
        refine_face_landmarks=True,
        min_detection_confidence=0.1,
        min_tracking_confidence=0.1) as holistic:

      image_height, image_width, _ = img.shape
      results = holistic.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))


      annotated_image = img.copy()
      # Draw segmentation on the image.
      # To improve segmentation around boundaries, consider applying a joint
      # bilateral filter to "results.segmentation_mask" with "image".
      condition = np.stack((results.segmentation_mask,) * 3, axis=-1) > 0.1
      bg_image = np.zeros(img.shape, dtype=np.uint8)
      bg_image[:] = (255, 255, 255)
      annotated_image = np.where(condition, annotated_image, bg_image)
      # Draw pose, left and right hands, and face landmarks on the image.

      mp_drawing.draw_landmarks(
        annotated_image,
        results.face_landmarks,
        mp_holistic.FACEMESH_TESSELATION,
        landmark_drawing_spec=None,
        connection_drawing_spec=mp_drawing_styles
        .get_default_face_mesh_tesselation_style())
      mp_drawing.draw_landmarks(
        annotated_image,
        results.left_hand_landmarks,
        mp_holistic.HAND_CONNECTIONS,
        landmark_drawing_spec=mp_drawing_styles.
        get_default_hand_landmarks_style())
      mp_drawing.draw_landmarks(
        annotated_image,
        results.right_hand_landmarks,
        mp_holistic.HAND_CONNECTIONS,
        landmark_drawing_spec=mp_drawing_styles.
        get_default_hand_landmarks_style())
      mp_drawing.draw_landmarks(
          annotated_image,
          results.pose_landmarks,
          mp_holistic.POSE_CONNECTIONS,
          landmark_drawing_spec=mp_drawing_styles.
          get_default_pose_landmarks_style())


      print(num)
      '''
      if results.right_hand_landmarks is None:
        print(results.right_hand_landmarks is None)
        print("x")
      else:
        for res in results.right_hand_landmarks:
          joint = np.zeros((21, 4))
          for j, lm in enumerate(res.landmark):
            joint[j] = [lm.x, lm.y, lm.z, lm.visibility]
            d = joint.flatten()
            print(d)
            data.append(d)
'''   
      keypoint_pos1 = []
      keypoint_pos2 = []
      data = []
      data2 = []
      if results.left_hand_landmarks:
        landmarks = results.left_hand_landmarks.landmark
       
        joint = np.zeros((24,4))
        for j,lm in enumerate(landmarks):
            joint[j] = [lm.x, lm.y, lm.z, lm.visibility]
          
            v1 = joint[[0,1,2,3,0,5,6,7,0,9,10,11,0,13,14,15,0,17,18,19], :3] # Parent joint
            v2 = joint[[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20], :3] # Child joint
            v = v2 - v1 
          
            v = v/np.linalg.norm(v, axis=1)[:, np.newaxis]
          
            angle = np.arccos(np.einsum('nt,nt->n',
                        v[[0,1,2,4,5,6,8,9,10,12,13,14,16,17,18],:], 
                        v[[1,2,3,5,6,7,9,10,11,13,14,15,17,18,19],:]))
            angle = np.degrees(angle)
            angle_label = np.array([angle], dtype=np.float32)
            angle_label = np.append(angle_label, idx)

            d = np.concatenate([joint.flatten()])

            data.append(d)

          #x = int(i.x * img.shape[1])
          #y = int(i.y * img.shape[0])
          #img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          #keypoint_pos1.append((x, y))

        print("left_angle")
        '''
      if results.right_hand_landmarks:
        landmarks = results.right_hand_landmarks.landmark
        for i in landmarks:
          x = int(i.x * img.shape[1])
          y = int(i.y * img.shape[0])
          img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          keypoint_pos1.append((x, y))
    '''
      if results.right_hand_landmarks:
        landmarks = results.right_hand_landmarks.landmark
       
        joint2 = np.zeros((24,4))
        for j,lm in enumerate(landmarks):
            joint2[j] = [lm.x, lm.y, lm.z, lm.visibility]
          
            v3 = joint2[[0,1,2,3,0,5,6,7,0,9,10,11,0,13,14,15,0,17,18,19], :3] # Parent joint
            v4 = joint2[[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20], :3] # Child joint
            v5 = v4 - v3 
          
            v5 = v5/np.linalg.norm(v5, axis=1)[:, np.newaxis]
          
            angle2 = np.arccos(np.einsum('nt,nt->n',
                        v5[[0,1,2,4,5,6,8,9,10,12,13,14,16,17,18],:], 
                        v5[[1,2,3,5,6,7,9,10,11,13,14,15,17,18,19],:]))
            angle2 = np.degrees(angle2)
            angle2_label = np.array([angle2], dtype=np.float32)
            angle2_label = np.append(angle2_label, idx)

            d2 = np.concatenate([joint2.flatten()])

            data2.append(d2)
      print("right_angle")

      if results.face_landmarks:
        landmarks = results.face_landmarks.landmark
        for i in MOUTH:
          x = int(landmarks[i].x * img.shape[1])
          y = int(landmarks[i].y * img.shape[0])
          img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          keypoint_pos1.append((x, y))
        for i in LEFT_EYE_INDEXES:
          x = int(landmarks[i].x * img.shape[1])
          y = int(landmarks[i].y * img.shape[0])
          img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          keypoint_pos1.append((x, y))
        for i in RIGHT_EYE_INDEXES:
          x = int(landmarks[i].x * img.shape[1])
          y = int(landmarks[i].y * img.shape[0])
          img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          keypoint_pos1.append((x, y))

      if results.pose_landmarks:
        landmarks = results.pose_landmarks.landmark
        for i in POSE:
          x = int(landmarks[i].x * img.shape[1])
          y = int(landmarks[i].y * img.shape[0])
          img=cv2.line(img,(int(x),int(y)),(int(x),int(y)),(0,0,255),5)
          keypoint_pos1.append((x, y))


      result.append(keypoint_pos1)
      print("왼손",angle)
      print("오른손",angle2)
      print("얼굴",result)
      #cv2.imwrite('tmp4/'+str(num)+'.png',img)

      num+=1

  result = np.array(result, dtype=object)
  np.save('왼쪽_D', result)
