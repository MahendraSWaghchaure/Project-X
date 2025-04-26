const Notifications = () => {
    return (
      <div className="notifications-page">
        <h1>Notifications</h1>
        <ul>
          <li>📢 Bus 12 will be delayed by 10 minutes today.</li>
          <li>📢 Route updated for Bus 15 due to roadwork.</li>
          {/* Example. Dynamically load notifications */}
        </ul>
      </div>
    );
  };
  
  export default Notifications;
  