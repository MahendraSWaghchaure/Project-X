package com.projectx.attendance.service;

import com.projectx.attendance.domain.Assessment;
import com.projectx.attendance.domain.StudentComment;
import com.projectx.attendance.dto.AcademicDtos.AssessmentRequest;
import com.projectx.attendance.dto.AcademicDtos.AssessmentResponse;
import com.projectx.attendance.dto.AcademicDtos.CommentRequest;
import com.projectx.attendance.dto.AcademicDtos.CommentResponse;
import com.projectx.attendance.repo.AssessmentRepository;
import com.projectx.attendance.repo.StudentCommentRepository;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-student assessments and teacher comments. History is capped at the newest 200 entries per call - a student's lifetime in one school is far below that. */
@Service
@Transactional
public class AcademicRecordService {

    static final int MAX_HISTORY = 200;

    private final StudentService studentService;
    private final TeacherScope teacherScope;
    private final AssessmentRepository assessmentRepository;
    private final StudentCommentRepository commentRepository;

    public AcademicRecordService(StudentService studentService, TeacherScope teacherScope, AssessmentRepository assessmentRepository,
                                 StudentCommentRepository commentRepository) {
        this.studentService = studentService;
        this.teacherScope = teacherScope;
        this.assessmentRepository = assessmentRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> listAssessments(Long schoolId, Long studentId) {
        teacherScope.requireAnyRelation(studentService.requireStudent(schoolId, studentId));
        return assessmentRepository
                .findByStudentId(studentId, PageRequest.of(0, MAX_HISTORY, Sort.by(Sort.Order.desc("examDate"), Sort.Order.desc("id"))))
                .stream().map(AssessmentResponse::from).toList();
    }

    public AssessmentResponse addAssessment(Long schoolId, Long studentId, AssessmentRequest request) {
        teacherScope.requireAnyRelation(studentService.requireStudent(schoolId, studentId));
        if (request.marksObtained().compareTo(request.maxMarks()) > 0) {
            throw new IllegalArgumentException("marksObtained cannot exceed maxMarks");
        }
        Assessment saved = assessmentRepository.save(new Assessment(schoolId, studentId, request.examName().trim(),
                request.subject().trim(), request.maxMarks(), request.marksObtained(), request.examDate(),
                CurrentUserContext.get().userId()));
        return AssessmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long schoolId, Long studentId) {
        teacherScope.requireClassTeacher(studentService.requireStudent(schoolId, studentId));
        return commentRepository
                .findByStudentId(studentId, PageRequest.of(0, MAX_HISTORY, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .stream().map(CommentResponse::from).toList();
    }

    public CommentResponse addComment(Long schoolId, Long studentId, CommentRequest request) {
        teacherScope.requireClassTeacher(studentService.requireStudent(schoolId, studentId));
        CurrentUser user = CurrentUserContext.get();
        StudentComment saved = commentRepository.save(
                new StudentComment(schoolId, studentId, request.text().trim(), user.userId(), user.email()));
        return CommentResponse.from(saved);
    }
}
