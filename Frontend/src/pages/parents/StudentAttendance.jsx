const StudentAttendance = () => {
    return (
      <div className="student-attendance-page">
        <h1>Student Attendance</h1>
        <ul>
          <li>🟢 Boarded: Morning (08:00 AM)</li>
          <li>🟢 Dropped: Evening (03:30 PM)</li>
          {/* Example. You can fetch and map attendance records dynamically */}
        </ul>
      </div>
    );
  };
  
  export default StudentAttendance;
  